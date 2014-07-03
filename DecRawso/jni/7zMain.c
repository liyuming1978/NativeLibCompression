/* 7zMain.c - Test application for 7z Decoder
2010-10-28 : Igor Pavlov : Public domain */

/*M///////////////////////////////////////////////////////////////////////////////////////
//
//  IMPORTANT: READ BEFORE DOWNLOADING, COPYING, INSTALLING OR USING.
//
//  By downloading, copying, installing or using the software you agree to this license.
//  If you do not agree to this license, do not download, install,
//  copy or use the software.
//
//
//                           License Agreement
//                For Open Source Computer Vision Library
//
// Copyright (C) 2014
// All rights reserved.
//
//Redistribution and use in source and binary forms, with or without
//modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.

//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
//ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
//WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
//DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
//(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
//LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
//ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//M*/

#include <jni.h>
#include <stdio.h>
#include <string.h>

#include "7zC/7z.h"
#include "7zC/7zAlloc.h"
#include "7zC/7zCrc.h"
#include "7zC/7zFile.h"
#include "7zC/7zVersion.h"

#ifndef USE_WINDOWS_FILE
/* for mkdir */
#ifdef _WIN32
#include <direct.h>
#else
#include <sys/stat.h>
#include <errno.h>
#endif
#endif

#include <android/log.h>
#define LOG_TAG "7z"
//#define msg_Dbg(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define msg_Dbg(...)

static ISzAlloc g_Alloc = { SzAlloc, SzFree };
static jboolean mWorkMode = JNI_TRUE;

static int Buf_EnsureSize(CBuf *dest, size_t size)
{
  if (dest->size >= size)
    return 1;
  Buf_Free(dest, &g_Alloc);
  return Buf_Create(dest, size, &g_Alloc);
}

#ifndef _WIN32

static Byte kUtf8Limits[5] = { 0xC0, 0xE0, 0xF0, 0xF8, 0xFC };

static Bool Utf16_To_Utf8(Byte *dest, size_t *destLen, const UInt16 *src, size_t srcLen)
{
  size_t destPos = 0, srcPos = 0;
  for (;;)
  {
    unsigned numAdds;
    UInt32 value;
    if (srcPos == srcLen)
    {
      *destLen = destPos;
      return True;
    }
    value = src[srcPos++];
    if (value < 0x80)
    {
      if (dest)
        dest[destPos] = (char)value;
      destPos++;
      continue;
    }
    if (value >= 0xD800 && value < 0xE000)
    {
      UInt32 c2;
      if (value >= 0xDC00 || srcPos == srcLen)
        break;
      c2 = src[srcPos++];
      if (c2 < 0xDC00 || c2 >= 0xE000)
        break;
      value = (((value - 0xD800) << 10) | (c2 - 0xDC00)) + 0x10000;
    }
    for (numAdds = 1; numAdds < 5; numAdds++)
      if (value < (((UInt32)1) << (numAdds * 5 + 6)))
        break;
    if (dest)
      dest[destPos] = (char)(kUtf8Limits[numAdds - 1] + (value >> (6 * numAdds)));
    destPos++;
    do
    {
      numAdds--;
      if (dest)
        dest[destPos] = (char)(0x80 + ((value >> (6 * numAdds)) & 0x3F));
      destPos++;
    }
    while (numAdds != 0);
  }
  *destLen = destPos;
  return False;
}

static SRes Utf16_To_Utf8Buf(CBuf *dest, const UInt16 *src, size_t srcLen)
{
  size_t destLen = 0;
  Bool res;
  Utf16_To_Utf8(NULL, &destLen, src, srcLen);
  destLen += 1;
  if (!Buf_EnsureSize(dest, destLen))
    return SZ_ERROR_MEM;
  res = Utf16_To_Utf8(dest->data, &destLen, src, srcLen);
  dest->data[destLen] = 0;
  return res ? SZ_OK : SZ_ERROR_FAIL;
}
#endif

static SRes Utf16_To_Char(CBuf *buf, const UInt16 *s, int fileMode)
{
  int len = 0;
  for (len = 0; s[len] != '\0'; len++);

  #ifdef _WIN32
  {
    int size = len * 3 + 100;
    if (!Buf_EnsureSize(buf, size))
      return SZ_ERROR_MEM;
    {
      char defaultChar = '_';
      BOOL defUsed;
      int numChars = WideCharToMultiByte(fileMode ?
          (
          #ifdef UNDER_CE
          CP_ACP
          #else
          AreFileApisANSI() ? CP_ACP : CP_OEMCP
          #endif
          ) : CP_OEMCP,
          0, s, len, (char *)buf->data, size, &defaultChar, &defUsed);
      if (numChars == 0 || numChars >= size)
        return SZ_ERROR_FAIL;
      buf->data[numChars] = 0;
      return SZ_OK;
    }
  }
  #else
  //fileMode = fileMode;
  return Utf16_To_Utf8Buf(buf, s, len);
  #endif
}

static WRes MyCreateDir(const UInt16 *name)
{
  #ifdef USE_WINDOWS_FILE
  
  return CreateDirectoryW(name, NULL) ? 0 : GetLastError();
  
  #else

  CBuf buf;
  WRes res;
  Buf_Init(&buf);
  RINOK(Utf16_To_Char(&buf, name, 1));

  res =
  #ifdef _WIN32
  _mkdir((const char *)buf.data)
  #else
  mkdir((const char *)buf.data, 0777)
  #endif
  == 0 ? 0 : errno;
  Buf_Free(&buf, &g_Alloc);
  return res;
  
  #endif
}

static WRes OutFile_OpenUtf16(CSzFile *p, const UInt16 *name)
{
  #ifdef USE_WINDOWS_FILE
  return OutFile_OpenW(p, name);
  #else
  CBuf buf;
  WRes res;
  Buf_Init(&buf);
  RINOK(Utf16_To_Char(&buf, name, 1));
  res = OutFile_Open(p, (const char *)buf.data);
  Buf_Free(&buf, &g_Alloc);
  return res;
  #endif
}

static SRes PrintString(const UInt16 *s)
{
  CBuf buf;
  SRes res;
  Buf_Init(&buf);
  res = Utf16_To_Char(&buf, s, 0);
  if (res == SZ_OK)
    fputs((const char *)buf.data, stdout);
  Buf_Free(&buf, &g_Alloc);
  return res;
}

static void UInt64ToStr(UInt64 value, char *s)
{
  char temp[32];
  int pos = 0;
  do
  {
    temp[pos++] = (char)('0' + (unsigned)(value % 10));
    value /= 10;
  }
  while (value != 0);
  do
    *s++ = temp[--pos];
  while (pos);
  *s = '\0';
}

static char *UIntToStr(char *s, unsigned value, int numDigits)
{
  char temp[16];
  int pos = 0;
  do
    temp[pos++] = (char)('0' + (value % 10));
  while (value /= 10);
  for (numDigits -= pos; numDigits > 0; numDigits--)
    *s++ = '0';
  do
    *s++ = temp[--pos];
  while (pos);
  *s = '\0';
  return s;
}

#define PERIOD_4 (4 * 365 + 1)
#define PERIOD_100 (PERIOD_4 * 25 - 1)
#define PERIOD_400 (PERIOD_100 * 4 + 1)

static void ConvertFileTimeToString(const CNtfsFileTime *ft, char *s)
{
  unsigned year, mon, day, hour, min, sec;
  UInt64 v64 = (ft->Low | ((UInt64)ft->High << 32)) / 10000000;
  Byte ms[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
  unsigned t;
  UInt32 v;
  sec = (unsigned)(v64 % 60); v64 /= 60;
  min = (unsigned)(v64 % 60); v64 /= 60;
  hour = (unsigned)(v64 % 24); v64 /= 24;

  v = (UInt32)v64;

  year = (unsigned)(1601 + v / PERIOD_400 * 400);
  v %= PERIOD_400;

  t = v / PERIOD_100; if (t ==  4) t =  3; year += t * 100; v -= t * PERIOD_100;
  t = v / PERIOD_4;   if (t == 25) t = 24; year += t * 4;   v -= t * PERIOD_4;
  t = v / 365;        if (t ==  4) t =  3; year += t;       v -= t * 365;

  if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0))
    ms[1] = 29;
  for (mon = 1; mon <= 12; mon++)
  {
    unsigned s = ms[mon - 1];
    if (v < s)
      break;
    v -= s;
  }
  day = (unsigned)v + 1;
  s = UIntToStr(s, year, 4); *s++ = '-';
  s = UIntToStr(s, mon, 2);  *s++ = '-';
  s = UIntToStr(s, day, 2);  *s++ = ' ';
  s = UIntToStr(s, hour, 2); *s++ = ':';
  s = UIntToStr(s, min, 2);  *s++ = ':';
  s = UIntToStr(s, sec, 2);
}

void PrintError(char *sz)
{
  //printf("\nERROR: %s\n", sz);
	msg_Dbg("lym = %s",sz);
}

#ifdef USE_WINDOWS_FILE
#define kEmptyAttribChar '.'
static void GetAttribString(UInt32 wa, Bool isDir, char *s)
{
  s[0] = (char)(((wa & FILE_ATTRIBUTE_DIRECTORY) != 0 || isDir) ? 'D' : kEmptyAttribChar);
  s[1] = (char)(((wa & FILE_ATTRIBUTE_READONLY) != 0) ? 'R': kEmptyAttribChar);
  s[2] = (char)(((wa & FILE_ATTRIBUTE_HIDDEN) != 0) ? 'H': kEmptyAttribChar);
  s[3] = (char)(((wa & FILE_ATTRIBUTE_SYSTEM) != 0) ? 'S': kEmptyAttribChar);
  s[4] = (char)(((wa & FILE_ATTRIBUTE_ARCHIVE) != 0) ? 'A': kEmptyAttribChar);
  s[5] = '\0';
}
#else
static void GetAttribString(UInt32 a, Bool b, char *s)
{
  s[0] = '\0';
}
#endif

//-------------liyuming add for lib copy due to system abi----------------------------
// GOOD and MATCH means: these libs can be mixed called
#define VALUE_MATCH		100000                      //do not modify it
#define VALUE_GOOD 		1000						//do not modify it
//these means: only no good or match , then use these libs
#define VALUE_NORMALGOOD 	100
#define VALUE_NORMAL 		10
#define VALUE_MIN			1
// means can not be used
#define	VALUE_REFUSE	0

typedef struct _StringWithLen
{
	UInt16* name;
	int len;
}stStringWithLen;

typedef struct _UsedLib
{
	stStringWithLen folderlibname;
	int index;
	int value;
}stUsedLib;

typedef int (*pfnValue)(stStringWithLen* pname);

static UInt16 filtername[100],fixname[100];
static stStringWithLen sFilterName={filtername,0},sFixName={fixname,0};
static int nofilter=1;

int STRCOMPARE(stStringWithLen* name1,stStringWithLen* name2)
{
	int i;
	if(name1->len!=name2->len)
		return 0;

	for(i=0;i<name1->len;i++)
	{
		if(name1->name[i]!=name2->name[i])
			return 0;
	}
	return 1;
}

int findmatch(stStringWithLen* foldername,stUsedLib* plib)
{
	int i=0;
	while(plib[i].folderlibname.name!=NULL)
	{
		if(STRCOMPARE(foldername,&(plib[i].folderlibname)))
			return i;
		i++;
	}
	//not find, add new
	msg_Dbg("find new =%d",foldername->len);
	plib[i].folderlibname.name = (UInt16*)calloc(foldername->len+1,sizeof(UInt16));
	memcpy(plib[i].folderlibname.name,foldername->name,foldername->len*sizeof(UInt16));
	plib[i].folderlibname.len = foldername->len;
	plib[i].value=0;

	return i;
}

void freestUsedLib(stUsedLib* plib)
{
	if(plib)
	{
		int i=0;
		while(plib[i].folderlibname.name!=NULL)
		{
			free(plib[i].folderlibname.name);
			i++;
		}
	}
}

int isx86(stStringWithLen* name)
{
	if(name->len==3)
	{
		if(name->name[0]=='x')
			return 1;
	}
	return 0;
}

int isx64(stStringWithLen* name)
{
	if(name->len==6)
	{
		if(name->name[0]=='x' && name->name[5]=='4')
			return 1;
	}
	return 0;
}

int ismips(stStringWithLen* name)
{
	if(name->len==4)
	{
		if(name->name[0]=='m')
			return 1;
	}
	return 0;
}

int ismips64(stStringWithLen* name)
{
	if(name->len==6)
	{
		if(name->name[0]=='m')
			return 1;
	}
	return 0;
}

int isarmeabi(stStringWithLen* name)
{
	if(name->len==7)
	{
		if(name->name[0]=='a')
			return 1;
	}
	return 0;
}

int isarmeabiv7a(stStringWithLen* name)
{
	if(name->len>=11)
	{
		if(name->name[10]=='a')
			return 1;
	}
	return 0;
}

int isarm64(stStringWithLen* name)
{
	if(name->len==5)
	{
		if(name->name[0]=='a')
			return 1;
	}
	return 0;
}

int is64bit(stStringWithLen* name)
{
	if(name->len>2)
	{
		if(name->name[name->len-2]=='6' && name->name[name->len-1]=='4')
			return 1;
	}
	return 0;
}


static int value_x86(stStringWithLen* folderabi)
{
	if(ismips(folderabi)||is64bit(folderabi))
		return VALUE_REFUSE;
	else if(isx86(folderabi))
		return VALUE_MATCH;
	else if(isarmeabi(folderabi))
		return VALUE_MIN;
	else if(isarmeabiv7a(folderabi))
		return VALUE_NORMAL;
}

static int value_x64(stStringWithLen* folderabi)
{
	if(ismips(folderabi)||ismips64(folderabi))
		return VALUE_REFUSE;
	else if(isx64(folderabi))
		return VALUE_MATCH;
	else if(isx86(folderabi))
		return VALUE_GOOD;
	else if(isarmeabi(folderabi))
		return VALUE_MIN;
	else if(isarmeabiv7a(folderabi))
		return VALUE_NORMAL;
	else if(isarm64(folderabi))
		return VALUE_NORMALGOOD;
}

static int value_mips(stStringWithLen* folderabi)
{
	if(isx86(folderabi)||is64bit(folderabi))
		return VALUE_REFUSE;
	else if(ismips(folderabi))
		return VALUE_MATCH;
	else if(isarmeabi(folderabi))
		return VALUE_MIN;
	else if(isarmeabiv7a(folderabi))
		return VALUE_NORMAL;
}

static int value_mips64(stStringWithLen* folderabi)
{
	if(isx86(folderabi)||isx64(folderabi))
		return VALUE_REFUSE;
	else if(ismips64(folderabi))
		return VALUE_MATCH;
	else if(ismips(folderabi))
		return VALUE_GOOD;
	else if(isarmeabi(folderabi))
		return VALUE_MIN;
	else if(isarmeabiv7a(folderabi))
		return VALUE_NORMAL;
	else if(isarm64(folderabi))
		return VALUE_NORMALGOOD;
}

static int value_aemeabi(stStringWithLen* folderabi)
{
	if(isx86(folderabi) ||is64bit(folderabi) || ismips(folderabi) || isarmeabiv7a(folderabi))
		return VALUE_REFUSE;
	else if(isarmeabi(folderabi))
		return VALUE_MATCH;
}

static int value_aemeabiv7a(stStringWithLen* folderabi)
{
	if(isx86(folderabi) ||is64bit(folderabi) || ismips(folderabi))
		return VALUE_REFUSE;
	else if(isarmeabiv7a(folderabi))
		return VALUE_MATCH;
	else if(isarmeabi(folderabi))
		return VALUE_GOOD;
}

static int value_arm64(stStringWithLen* folderabi)
{
	if(isx86(folderabi) ||isx64(folderabi) || ismips(folderabi)|| ismips64(folderabi))
		return VALUE_REFUSE;
	else if(isarm64(folderabi))
		return VALUE_MATCH;
	else if(isarmeabiv7a(folderabi))
		return VALUE_NORMALGOOD;
	else if(isarmeabi(folderabi))
		return VALUE_NORMAL;
}


static int caculate_libs(pfnValue value_libs,stStringWithLen* folderabi,stStringWithLen* foldername,int index,stUsedLib* plib)
{
	int findindex;
	int value = value_libs(folderabi);

	if(value == VALUE_REFUSE)
		return 0;
	msg_Dbg("caculate_libs =%d ,%d",folderabi->len,foldername->len);
	findindex = findmatch(foldername,plib);
	if(plib[findindex].value<value)
	{
		plib[findindex].index = index;
		plib[findindex].value = value;
	}
	return value;
}

int STR_STARTWITH(stStringWithLen* strname,stStringWithLen* subname)
{
	int i;
	if(strname->len<subname->len)
		return 0;

	for(i=0;i<subname->len-1;i++)  //do not include the last 0
	{
		msg_Dbg("STR_STARTWITH index=%d %c--%c",i,strname->name[i],subname->name[i]);
		if(strname->name[i]!=subname->name[i])
		{
			msg_Dbg("STR_STARTWITH index=%d %c!=%c",i,strname->name[i],subname->name[i]);
			return 0;
		}
	}
	return 1;
}

int check_filter(stStringWithLen* fname)
{
	if(0==sFilterName.len)
		return 1;  //if not set filter, not block

	if(STR_STARTWITH(fname,&sFilterName))
	{
		msg_Dbg("check_filter %d,%d",fname->len,sFixName.len);
		if(STRCOMPARE(fname,&sFixName))
		{
			nofilter = 0;
			return 1;
		}
		else
			return 0;
	}

	return 1;
}


//-------------liyuming add end-----------------------------------------------------------
#if 0
#ifndef ANDROID22
#include<cpu-features.h>
#endif
JNIEXPORT jint JNICALL Java_com_library_decrawso_DecRawso_GetCpufamily(JNIEnv * env, jclass thiz)
{
#ifndef ANDROID22
	AndroidCpuFamily cpuFamily = android_getCpuFamily();
	if(ANDROID_CPU_FAMILY_X86 == cpuFamily)
		return 1;
	else if(ANDROID_CPU_FAMILY_ARM == cpuFamily)
		return 2;
	else
#endif
		return 0;
}

char* readline(FILE* f)
{
    char* line = NULL;
    int c;
    int len = 0;

    while ( (c = fgetc(f) ) != EOF && c != '/n')
    {
        line = (char*) realloc(line, sizeof(char) * (len + 2) );
        line[len++] = c;
        line[len] = '/0';
    }

    return line;
}

JNIEXPORT jint JNICALL Java_com_library_decrawso_DecRawso_GetCpufamily(JNIEnv * env, jclass thiz)
{
	FILE* fp;
	char* s;
	fp= fopen("/proc/cpuinfo","r");

	while((s=readline(fp))!=NULL)
	{
		msg_Dbg("cpu = %s",s);
		if(strncmp(s,"cpu family",10)==0)
		{
			free(s);
			fclose(fp);
			return 1;
		}
		free(s);
	}

	fclose(fp);
	return 0;
}
#endif

JNIEXPORT void JNICALL Java_com_library_decrawso_DecRawso_SetFilter
(JNIEnv * env, jclass thiz,jstring jfilter,jstring jfix)
{
	const jchar *sfilter=NULL,*sfix=NULL,*sbackup=NULL;
	int nfilter,nfix,nbackup;
	if(jfilter==NULL ||jfix==NULL)
	{
		sFilterName.len = 0;
		return;
	}

	sfilter = (*env)->GetStringChars(env,jfilter, NULL);
	nfilter = (*env)->GetStringLength(env,jfilter);
	sfix = (*env)->GetStringChars(env,jfix, NULL);
	nfix = (*env)->GetStringLength(env,jfix);

	if(sfilter)
	{
		memcpy(sFilterName.name,sfilter,nfilter*sizeof(jchar));
		sFilterName.len = nfilter+1;  //include the 0
		sFilterName.name[nfilter]=0;
		(*env)->ReleaseStringChars(env,jfilter,sfilter);
	}
	if(sfix)
	{
		memcpy(sFixName.name,sfix,nfix*sizeof(jchar));
		sFixName.len=nfix+1;
		sFixName.name[nfix]=0;
		(*env)->ReleaseStringChars(env,jfix,sfix);
	}
}

JNIEXPORT jboolean JNICALL Java_com_library_decrawso_DecRawso_IsArmMode(JNIEnv * env, jclass thiz)
{
	return mWorkMode;
}

JNIEXPORT int JNICALL Java_com_library_decrawso_DecRawso_Decode
(JNIEnv * env, jclass thiz,jobject assetManager,jstring jinpath,jstring joutpath,jstring jabi)
{
  CFileInStream archiveStream;
  CLookToRead lookStream;
  CSzArEx db;
  SRes res;
  ISzAlloc allocImp;
  ISzAlloc allocTempImp;
  UInt16 *temp = NULL;
  size_t tempSize = 0;
  pfnValue value_libs;
  stUsedLib* plib = NULL;
  const char* infilename = NULL;
  int x86_mips=0;
  int has_match=0;
  //int has_x86=0;

  const jchar* outpath = (*env)->GetStringChars(env,joutpath, NULL);
  jsize npathlen= (*env)->GetStringLength(env,joutpath);
  if(jinpath!=NULL)
  {
	  infilename = (*env)->GetStringUTFChars(env,jinpath, NULL);
  }

  const jchar * abi = (*env)->GetStringChars(env,jabi, NULL);
  stStringWithLen strabi;
  strabi.name = (UInt16*)abi;
  strabi.len = (*env)->GetStringLength(env,jabi);
  if(isx86(&strabi))
  {
	  value_libs = value_x86;
	  x86_mips = 1;
  }
  else if(isx64(&strabi))
  {
	  value_libs = value_x64;
	  x86_mips = 1;
  }
  else if(ismips(&strabi))
  {
	  value_libs = value_mips;
	  x86_mips = 1;
  }
  else if(ismips64(&strabi))
  {
	  value_libs = value_mips64;
	  x86_mips = 1;
  }
  else if(isarm64(&strabi))
	  value_libs = value_arm64;
  else if(isarmeabiv7a(&strabi))
	  value_libs = value_aemeabiv7a;
  else// if(isarmeabi(&strabi))
	  value_libs = value_aemeabi;
  (*env)->ReleaseStringChars(env,jabi,abi);

#ifndef ANDROID22
  if(assetManager!=NULL)
  {
	  archiveStream.file.mgr = AAssetManager_fromJava(env, assetManager);
	  infilename = "rawso";
  }
  else
#endif
	  archiveStream.file.mgr = NULL;

  allocImp.Alloc = SzAlloc;
  allocImp.Free = SzFree;

  allocTempImp.Alloc = SzAllocTemp;
  allocTempImp.Free = SzFreeTemp;

  if (InFile_Open(&archiveStream.file, infilename))
  {
    PrintError("can not open input file");
    goto MYERROR;
  }

  FileInStream_CreateVTable(&archiveStream);
  LookToRead_CreateVTable(&lookStream, False);
  
  lookStream.realStream = &archiveStream.s;
  LookToRead_Init(&lookStream);

  CrcGenerateTable();

  SzArEx_Init(&db);
  res = SzArEx_Open(&db, &lookStream.s, &allocImp, &allocTempImp);
  if (res == SZ_OK)
  {
    int listCommand = 0, testCommand = 0, extractCommand = 0, fullPaths = 0;
    extractCommand = 1; fullPaths = 1;  //dec with full path
    int checkfilter = 1;
	if(0==sFilterName.len)
		nofilter = 0;
	else
		nofilter = 1;

    if (res == SZ_OK)
    {
      UInt32 i,m;

      /*
      if you need cache, use these 3 variables.
      if you use external function, you can make these variable as static.
      */
      UInt32 blockIndex = 0xFFFFFFFF; /* it can have any value before first call (if outBuffer = 0) */
      Byte *outBuffer = 0; /* it must be 0 before first call for each new archive. */
      size_t outBufferSize = 0;  /* it can have any value before first call (if outBuffer = 0) */

      plib = (stUsedLib*)calloc(db.db.NumFiles,sizeof(stUsedLib));
CHECK_AGAIN:
      for (i = 0; i < db.db.NumFiles; i++)
      {
    	  UInt16 tmpname[100];
    	  stStringWithLen folderabi;
    	  stStringWithLen foldername;
    	  const CSzFileItem *f = db.db.Files + i;
    	  int namelen = SzArEx_GetFileNameUtf16(&db, i, tmpname);
    	  int valuelib;

    	  if(f->IsDir)
    		  continue;
    	  for(m=0;m<namelen;m++)
    	  {
    		  if(m>1 && (tmpname[m]=='/'||tmpname[m]=='\\'))
    		  {
    			  folderabi.name = tmpname;
    			  folderabi.len = m;
    			  tmpname[m]=0;
    			  foldername.name = tmpname+m+1;
    			  foldername.len = namelen-m-1;
    			  foldername.name[foldername.len]=0;
    			  break;
    		  }
    		  //case-sensitive: filename
    	  }
    	  if(checkfilter)
    	  {
			  if(!check_filter(&foldername))
				  continue;
    	  }

    	  valuelib = caculate_libs(value_libs,&folderabi,&foldername,i,plib);
    	  msg_Dbg("last char =%c",foldername.name[foldername.len-2]);
    	  //if there are some .so in x86 folder, only copy x86 folder libs
    	  if(valuelib>=VALUE_GOOD && foldername.len>4 && foldername.name[foldername.len-4]=='.'
    			  && foldername.name[foldername.len-3]=='s' && foldername.name[foldername.len-2]=='o')
    		  has_match = 1;
    	  //if(isx86(&folderabi))
    		//  has_x86 = 1;
      }

      if(nofilter)
      {
    	  msg_Dbg("nofilter");
    	  memset(plib,0,sizeof(plib));
    	  checkfilter = 0;
    	  nofilter = 0;
    	  goto CHECK_AGAIN;
      }
      //if(!has_x86)
    	//  goto MYERROR;

      if(!x86_mips ||(x86_mips && !has_match))  //if not x86, or x86 but nomatch, work at arm mode
    	  mWorkMode = JNI_TRUE;
      else
    	  mWorkMode = JNI_FALSE;

      //for (i = 0; i < db.db.NumFiles; i++)
      m=0;
      while(plib[m].folderlibname.name!=NULL)
      {
        size_t offset = 0;
        size_t outSizeProcessed = 0;
        const CSzFileItem *f;
        size_t len;
        stStringWithLen foldername;
        int valuelib;

        i=plib[m].index;
        foldername = plib[m].folderlibname;
        valuelib = plib[m].value;
        m++;
        f = db.db.Files + i;
        if (listCommand == 0 && f->IsDir && !fullPaths)
          continue;
        // if has match, continue  .. only decode match(or good) lib
		if(has_match && valuelib<VALUE_GOOD)
			continue;

        //len = SzArEx_GetFileNameUtf16(&db, i, NULL);
        len = foldername.len;

        if (len > tempSize)
        {
          SzFree(NULL, temp);
          tempSize = len;
          temp = (UInt16 *)SzAlloc(NULL, (tempSize+npathlen) * sizeof(temp[0]));
          if (temp == 0)
          {
            res = SZ_ERROR_MEM;
            break;
          }
        }

        memcpy(temp,outpath,npathlen*sizeof(temp[0]));
        //SzArEx_GetFileNameUtf16(&db, i, temp+npathlen);
        memcpy(temp+npathlen,foldername.name,len*sizeof(temp[0]));

        if (listCommand)
        {
          char attr[8], s[32], t[32];

          GetAttribString(f->AttribDefined ? f->Attrib : 0, f->IsDir, attr);

          UInt64ToStr(f->Size, s);
          if (f->MTimeDefined)
            ConvertFileTimeToString(&f->MTime, t);
          else
          {
            size_t j;
            for (j = 0; j < 19; j++)
              t[j] = ' ';
            t[j] = '\0';
          }
          
          printf("%s %s %10s  ", t, attr, s);
          res = PrintString(temp);
          if (res != SZ_OK)
            break;
          if (f->IsDir)
            printf("/");
          printf("\n");
          continue;
        }
        fputs(testCommand ?
            "Testing    ":
            "Extracting ",
            stdout);
        res = PrintString(temp);
        if (res != SZ_OK)
          break;
        if (f->IsDir)
          printf("/");
        else
        {
          res = SzArEx_Extract(&db, &lookStream.s, i,
              &blockIndex, &outBuffer, &outBufferSize,
              &offset, &outSizeProcessed,
              &allocImp, &allocTempImp);
          if (res != SZ_OK)
            break;
        }
        if (!testCommand)
        {
          CSzFile outFile;
          size_t processedSize;
          size_t j;
          UInt16 *name = (UInt16 *)temp;
          const UInt16 *destPath = (const UInt16 *)name;
          for (j = 0; name[j] != 0; j++)
            if (name[j] == '/')
            {
              if (fullPaths)
              {
                name[j] = 0;
                MyCreateDir(name);
                name[j] = CHAR_PATH_SEPARATOR;
              }
              else
                destPath = name + j + 1;
            }
    
          if (f->IsDir)
          {
            MyCreateDir(destPath);
            printf("\n");
            continue;
          }
          else if (OutFile_OpenUtf16(&outFile, destPath))
          {
            PrintError("can not open output file");
            res = SZ_ERROR_WRITE;
            break;
          }
          processedSize = outSizeProcessed;
          if (File_Write(&outFile, outBuffer + offset, &processedSize) != 0 || processedSize != outSizeProcessed)
          {
            PrintError("can not write output file");
            res = SZ_ERROR_WRITE;
            break;
          }
          if (File_Close(&outFile))
          {
            PrintError("can not close output file");
            res = SZ_ERROR_WRITE;
            break;
          }
          #ifdef USE_WINDOWS_FILE
          if (f->AttribDefined)
            SetFileAttributesW(destPath, f->Attrib);
          #endif
        }
        printf("\n");
      }
      IAlloc_Free(&allocImp, outBuffer);
    }
  }
  SzArEx_Free(&db, &allocImp);
  SzFree(NULL, temp);
  freestUsedLib(plib);

  File_Close(&archiveStream.file);
  if (res == SZ_OK)
  {
    printf("\nEverything is Ok\n");
    (*env)->ReleaseStringChars(env,joutpath,outpath);
    if(jinpath!=NULL)
    {
    	(*env)->ReleaseStringUTFChars(env,jinpath,infilename);
    }
    return 0;
  }
  if (res == SZ_ERROR_UNSUPPORTED)
    PrintError("decoder doesn't support this archive");
  else if (res == SZ_ERROR_MEM)
    PrintError("can not allocate memory");
  else if (res == SZ_ERROR_CRC)
    PrintError("CRC error");
  else
    printf("\nERROR #%d\n", res);

 MYERROR:
  (*env)->ReleaseStringChars(env,joutpath,outpath);
  if(jinpath!=NULL)
  {
  	(*env)->ReleaseStringUTFChars(env,jinpath,infilename);
  }
  if(res!=0)
	  return res;
  else
	  return 1;
}

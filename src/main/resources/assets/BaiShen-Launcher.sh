#!/usr/bin/env bash

set -e

# Switch message language
if [ -z "${LANG##zh_*}" ]; then
  _BSL_USE_CHINESE=true
else
  _BSL_USE_CHINESE=false
fi

# _BSL_OS
case "$OSTYPE" in
  linux*)
    _BSL_OS="linux";;
  darwin*)
    _BSL_OS="osx";;
  msys*|cygwin*)
    _BSL_OS="windows";;
  *)
    _BSL_OS="unknown";;
esac

# Normalize _BSL_ARCH
case "$(uname -m)" in
  x86_64|x86-64|amd64|em64t|x64)
    _BSL_ARCH="x86_64";;
  x86_32|x86-32|x86|ia32|i386|i486|i586|i686|i86pc|x32)
    _BSL_ARCH="x86";;
  arm64|aarch64|armv8*|armv9*)
    _BSL_ARCH="arm64";;
  arm|arm32|aarch32|armv7*)
    _BSL_ARCH="arm32";;
  loongarch64)
    _BSL_ARCH="loongarch64";;
  *)
    _BSL_ARCH="unknown";;
esac

# Self path
_BSL_PATH="${BASH_SOURCE[0]}"
_BSL_DIR=$(dirname "$_BSL_PATH")

if [ "$_BSL_OS" == "windows" ]; then
  _BSL_JAVA_EXE_NAME="java.exe"
else
  _BSL_JAVA_EXE_NAME="java"
fi

# _BSL_VM_OPTIONS
if [ -n "${BSL_JAVA_OPTS+x}" ]; then
  _BSL_VM_OPTIONS=${BSL_JAVA_OPTS}
else
  _BSL_VM_OPTIONS="-XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=15"
fi

# First, find Java in BSL_JAVA_HOME
if [ -n "${BSL_JAVA_HOME+x}" ]; then
  if [ -x "$BSL_JAVA_HOME/bin/$_BSL_JAVA_EXE_NAME" ]; then
    "$BSL_JAVA_HOME/bin/$_BSL_JAVA_EXE_NAME" $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
    exit 0
  else
    if [ "$_BSL_USE_CHINESE" == true ]; then
      echo "环境变量 BSL_JAVA_HOME 的值无效，请设置为合法的 Java 路径。" 1>&2
    else
      echo "The value of the environment variable BSL_JAVA_HOME is invalid, please set it to a valid Java path." 1>&2
    fi
    exit 1
  fi
fi

# Find Java in BSL_DIR
case "$_BSL_ARCH" in
  x86_64)
    if [ -x "$_BSL_DIR/jre-x64/bin/$_BSL_JAVA_EXE_NAME" ]; then
      "$_BSL_DIR/jre-x64/bin/$_BSL_JAVA_EXE_NAME" $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
      exit 0
    fi
    if [ -x "$_BSL_DIR/jre-x86/bin/$_BSL_JAVA_EXE_NAME" ]; then
      "$_BSL_DIR/jre-x86/bin/$_BSL_JAVA_EXE_NAME" $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
      exit 0
    fi
    ;;
  x86)
    if [ -x "$_BSL_DIR/jre-x86/bin/$_BSL_JAVA_EXE_NAME" ]; then
      "$_BSL_DIR/jre-x86/bin/$_BSL_JAVA_EXE_NAME" $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
      exit 0
    fi
    ;;
  arm64)
    if [ -x "$_BSL_DIR/jre-arm64/bin/$_BSL_JAVA_EXE_NAME" ]; then
      "$_BSL_DIR/jre-arm64/bin/$_BSL_JAVA_EXE_NAME" $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
      exit 0
    fi
    ;;
  arm32)
    if [ -x "$_BSL_DIR/jre-arm32/bin/$_BSL_JAVA_EXE_NAME" ]; then
      "$_BSL_DIR/jre-arm32/bin/$_BSL_JAVA_EXE_NAME" $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
      exit 0
    fi
    ;;
  loongarch64)
    if [ -x "$_BSL_DIR/jre-loongarch64/bin/$_BSL_JAVA_EXE_NAME" ]; then
      "$_BSL_DIR/jre-loongarch64/bin/$_BSL_JAVA_EXE_NAME" $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
      exit 0
    fi
    ;;
esac

# Find Java in JAVA_HOME
if [ -f "$JAVA_HOME/bin/$_BSL_JAVA_EXE_NAME" ]; then
  "$JAVA_HOME/bin/$_BSL_JAVA_EXE_NAME" $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
  exit 0
fi

# Find Java in PATH
if [ -x "$(command -v $_BSL_JAVA_EXE_NAME)" ]; then
  $_BSL_JAVA_EXE_NAME $_BSL_VM_OPTIONS -jar "$_BSL_PATH"
  exit 0
fi

# Java not found

if [[ "$_BSL_OS" == "unknown" || "$_BSL_ARCH" == "unknown" ]]; then
  if [ "$_BSL_USE_CHINESE" == true ]; then
    echo "运行 BSL 需要 BaiShen-Launcher 运行时环境，请安装 Java 并设置环境变量后重试。" 1>&2
  else
    echo "运行 BSL 需要 BaiShen-Launcher 运行时环境，请安装 Java 并设置环境变量后重试。" 1>&2
  fi
  exit 1
fi

if [[ "$_BSL_ARCH" == "loongarch64" ]]; then
  if [ "$_BSL_USE_CHINESE" == true ]; then
    echo "运行 BaiShen-Launcher 需要 Java 运行时环境，请安装龙芯 JDK8 (https://docs.BSL.net/downloads/loongnix.html) 并设置环境变量后重试。" 1>&2
  else
    echo "运行 BaiShen-Launcher 需要 Java 运行时环境，请安装龙芯 JDK8 (https://docs.BSL.net/downloads/loongnix.html) 并设置环境变量后重试。" 1>&2
  fi
  exit 1
fi


case "$_BSL_OS" in
  linux)
    _BSL_DOWNLOAD_PAGE_OS="linux";;
  osx)
    _BSL_DOWNLOAD_PAGE_OS="macos";;
  windows)
    _BSL_DOWNLOAD_PAGE_OS="windows";;
  *)
    echo "Unknown os: $_BSL_OS" 1>&2
    exit 1
    ;;
esac

case "$_BSL_ARCH" in
  arm64)
    _BSL_DOWNLOAD_PAGE_ARCH="arm64";;
  arm32)
    _BSL_DOWNLOAD_PAGE_ARCH="arm32";;
  x86_64)
    _BSL_DOWNLOAD_PAGE_ARCH="x86_64";;
  x86)
    _BSL_DOWNLOAD_PAGE_ARCH="x86";;
  *)
    echo "Unknown architecture: $_BSL_ARCH" 1>&2
    exit 1
    ;;
esac

_BSL_DOWNLOAD_PAGE="https://launcher.白神遥.cn/"

if [ "$_BSL_USE_CHINESE" == true ]; then
  echo "运行 BaiShen-Launcher 需要 Java 运行时环境，请安装 Java 并设置环境变量后重试。" 1>&2
  echo "$_BSL_DOWNLOAD_PAGE" 1>&2
else
  echo "运行 BaiShen-Launcher 需要 Java 运行时环境，请安装 Java 并设置环境变量后重试。" 1>&2
  echo "$_BSL_DOWNLOAD_PAGE" 1>&2
fi
exit 1

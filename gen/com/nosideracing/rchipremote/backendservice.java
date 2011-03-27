/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/kevin/code/msremote/src/com/nosideracing/rchipremote/backendservice.aidl
 */
package com.nosideracing.rchipremote;
public interface backendservice extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.nosideracing.rchipremote.backendservice
{
private static final java.lang.String DESCRIPTOR = "com.nosideracing.rchipremote.backendservice";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.nosideracing.rchipremote.backendservice interface,
 * generating a proxy if needed.
 */
public static com.nosideracing.rchipremote.backendservice asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.nosideracing.rchipremote.backendservice))) {
return ((com.nosideracing.rchipremote.backendservice)iin);
}
return new com.nosideracing.rchipremote.backendservice.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_sendCmd:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
boolean _result = this.sendCmd(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setNotification:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
boolean _result = this.setNotification(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_clearNotifications:
{
data.enforceInterface(DESCRIPTOR);
this.clearNotifications();
reply.writeNoException();
return true;
}
case TRANSACTION_startMusicUpdating:
{
data.enforceInterface(DESCRIPTOR);
this.startMusicUpdating();
reply.writeNoException();
return true;
}
case TRANSACTION_stopMusicUpdating:
{
data.enforceInterface(DESCRIPTOR);
this.stopMusicUpdating();
reply.writeNoException();
return true;
}
case TRANSACTION_getArtest:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getArtest();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getAlbum:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getAlbum();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getSongName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getSongName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getTimeElapised:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getTimeElapised();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getSongLength:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getSongLength();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getIsPlaying:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getIsPlaying();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setKtorrentNotifications:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
boolean _result = this.setKtorrentNotifications(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getRootValue:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.getRootValue(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getHostNames:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getHostNames();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_UpdateSongInfo_Once:
{
data.enforceInterface(DESCRIPTOR);
this.UpdateSongInfo_Once();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.nosideracing.rchipremote.backendservice
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public boolean sendCmd(java.lang.String cmd, java.lang.String cmdText) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(cmd);
_data.writeString(cmdText);
mRemote.transact(Stub.TRANSACTION_sendCmd, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean setNotification(java.lang.String tickerString, java.lang.String notificationTitle, java.lang.String noticicationText) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(tickerString);
_data.writeString(notificationTitle);
_data.writeString(noticicationText);
mRemote.transact(Stub.TRANSACTION_setNotification, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void clearNotifications() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_clearNotifications, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void startMusicUpdating() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_startMusicUpdating, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void stopMusicUpdating() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stopMusicUpdating, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public java.lang.String getArtest() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getArtest, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getAlbum() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAlbum, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getSongName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSongName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getTimeElapised() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getTimeElapised, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getSongLength() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSongLength, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int getIsPlaying() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getIsPlaying, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean setKtorrentNotifications(boolean ktornot) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((ktornot)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setKtorrentNotifications, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getRootValue(java.lang.String hn) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(hn);
mRemote.transact(Stub.TRANSACTION_getRootValue, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getHostNames() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getHostNames, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void UpdateSongInfo_Once() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_UpdateSongInfo_Once, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_sendCmd = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_setNotification = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_clearNotifications = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_startMusicUpdating = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_stopMusicUpdating = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getArtest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_getAlbum = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_getSongName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_getTimeElapised = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getSongLength = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getIsPlaying = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_setKtorrentNotifications = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_getRootValue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_getHostNames = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_UpdateSongInfo_Once = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
}
public boolean sendCmd(java.lang.String cmd, java.lang.String cmdText) throws android.os.RemoteException;
public boolean setNotification(java.lang.String tickerString, java.lang.String notificationTitle, java.lang.String noticicationText) throws android.os.RemoteException;
public void clearNotifications() throws android.os.RemoteException;
public void startMusicUpdating() throws android.os.RemoteException;
public void stopMusicUpdating() throws android.os.RemoteException;
public java.lang.String getArtest() throws android.os.RemoteException;
public java.lang.String getAlbum() throws android.os.RemoteException;
public java.lang.String getSongName() throws android.os.RemoteException;
public java.lang.String getTimeElapised() throws android.os.RemoteException;
public java.lang.String getSongLength() throws android.os.RemoteException;
public int getIsPlaying() throws android.os.RemoteException;
public boolean setKtorrentNotifications(boolean ktornot) throws android.os.RemoteException;
public java.lang.String getRootValue(java.lang.String hn) throws android.os.RemoteException;
public java.lang.String getHostNames() throws android.os.RemoteException;
public void UpdateSongInfo_Once() throws android.os.RemoteException;
}

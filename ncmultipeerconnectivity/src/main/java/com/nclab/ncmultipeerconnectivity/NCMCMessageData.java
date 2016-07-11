package com.nclab.ncmultipeerconnectivity;

import java.util.ArrayList;
import java.util.List;

/**
 * data to be sent or received
 */
class NCMCMessageData {
    private String  deviceUUID;
    private  List<byte[]> dataArray;
    boolean isReliable;

    public NCMCMessageData(String _deviceUUID, boolean _isReliable) {
        this.deviceUUID = _deviceUUID;
        this.dataArray = new ArrayList<>();
        this.isReliable = _isReliable;
    }

    public String getDeviceUUID() {
        return this.deviceUUID;
    }

    public void clearData(){
        this.dataArray.clear();
    }

    public void addData(byte[] array){
        this.dataArray.add(array);
    }

    public byte[] getFullData(){
        byte[] retArray;
        int totalSize = 0;

        for(int i=0; i < this.dataArray.size();i++){
            totalSize = totalSize + this.dataArray.get(i).length;
        }

        int copuCounter = 0;
        if(totalSize > 0) {
            retArray = new byte[totalSize];
            for(int ii=0; ii < this.dataArray.size();ii++){
                byte[] tmpArr = this.dataArray.get(ii);
                System.arraycopy(tmpArr, 0, retArray,copuCounter,tmpArr.length);
                copuCounter = copuCounter + tmpArr.length;
            }
        }else{
            retArray = new byte[]{};
        }
        return retArray;
    }
}

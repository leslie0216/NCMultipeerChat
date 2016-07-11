package com.nclab.ncmultipeerconnectivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Android will split long data from central to peripheral, this class is used to save those chunks and combine them.
 */
class NCMCPeripheralWriteRequestData {
    private String  m_deviceAddress;
    private String m_UUID;
    private boolean m_isCharacter;
    List<byte[]> byteArray;
    public NCMCPeripheralWriteRequestData(String address,String uuid, boolean isCharacter){
        m_deviceAddress = address;
        m_UUID = uuid;
        byteArray = new ArrayList<>();
        m_isCharacter = isCharacter;
    }

    public boolean isCharacter(){return m_isCharacter;}
    public String getUUID(){return m_UUID;}
    public String getDeviceAddress(){return m_deviceAddress;}
    public void clearData(){
        byteArray.clear();
    }
    public void addData(byte[] array){
        byteArray.add(array);
    }

    public byte[] getFullData(){
        byte[] retArray;
        int totalSize = 0;

        for(int i=0; i < byteArray.size();i++){
            totalSize = totalSize + byteArray.get(i).length;
        }

        int copuCounter = 0;
        if(totalSize > 0) {
            retArray = new byte[totalSize];
            for(int ii=0; ii < byteArray.size();ii++){
                byte[] tmpArr = byteArray.get(ii);
                System.arraycopy(tmpArr, 0, retArray,copuCounter,tmpArr.length);
                copuCounter = copuCounter + tmpArr.length;
            }
        }else{
            retArray = new byte[]{};
        }
        return retArray;
    }
}

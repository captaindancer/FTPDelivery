package com.wind.openmeeting.deliver.log;

import com.sun.jna.Structure;

/**
 * 性能计数参数结构，通过JNA传递的数据结构定义不能为内部类
 * @author songchen
 *
 */
public class PerformaceInfo extends Structure{
    public int VMUsedMemory=0xffffffff;
    public int TotalRequest=0xffffffff;
    public int AvgRequest=0xffffffff;
    public int TotalTraffic=0xffffffff;
    public int AvgTraffic=0xffffffff;
    public int Reserve01=0xffffffff;
    public int Reserve02=0xffffffff;
    public int Reserve03=0xffffffff;
    public int Reserve04=0xffffffff;
    public int Reserve05=0xffffffff;
    public int Reserve06=0xffffffff;
    public int Reserve07=0xffffffff;
    public int Reserve08=0xffffffff;
    public int Reserve09=0xffffffff;
    public int Reserve10=0xffffffff;

}

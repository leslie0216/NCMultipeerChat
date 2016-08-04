package com.nclab.ncmultipeerchat;

import java.util.ArrayList;
import java.util.List;

/**
 * ping information
 */
public class PingInfo {
    public int m_token;
    public long m_startTime;
    public List<Double> m_timeIntervals;
    public int m_totalCount;
    public int m_currentCount;
    public int m_number;

    PingInfo() {
        m_timeIntervals = new ArrayList<>();
    }
}

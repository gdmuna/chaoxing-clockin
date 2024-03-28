package com.cheng.xxtsign.enums;

public enum LocationSignEnum {

    GDMU_A(113.868723, 22.929767),
    GDMU_B(113.869132, 22.929971),
    GDMU_C(113.869657, 22.930204),
    GDMU_D(113.869882, 22.929825),
    GDMU_E(113.870161, 22.929509),
    GDMU_F(113.870196, 22.929168),
    GDMU_G(113.870169, 22.928822),
    GDMU_H(113.870107, 22.928327),
    // 实验楼
    GDMU_S(113.869437, 22.928003),
    ;

    // 经度
    private double longitude;
    // 纬度
    private double latitude;

    LocationSignEnum(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}

package com.ee.lab.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class HardwareSpec {

    @Column(name = "cpu_cores", nullable = false)
    private int cpuCores;

    @Column(name = "ram_gb", nullable = false)
    private int ramGb;

    @Column(name = "storage_type", length = 32)
    private String storageType;

    public HardwareSpec() {
    }

    public HardwareSpec(int cpuCores, int ramGb, String storageType) {
        this.cpuCores = cpuCores;
        this.ramGb = ramGb;
        this.storageType = storageType;
    }

    public int getCpuCores() { return cpuCores; }
    public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }

    public int getRamGb() { return ramGb; }
    public void setRamGb(int ramGb) { this.ramGb = ramGb; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HardwareSpec that = (HardwareSpec) o;
        return cpuCores == that.cpuCores && ramGb == that.ramGb && Objects.equals(storageType, that.storageType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpuCores, ramGb, storageType);
    }

    @Override
    public String toString() {
        return "HardwareSpec{" + "cores=" + cpuCores + ", ram=" + ramGb + "GB, storage='" + storageType + '\'' + '}';
    }
}

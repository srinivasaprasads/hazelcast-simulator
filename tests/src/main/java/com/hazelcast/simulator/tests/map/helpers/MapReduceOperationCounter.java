package com.hazelcast.simulator.tests.map.helpers;

import java.io.Serializable;

public class MapReduceOperationCounter implements Serializable {

    public long mapReduce;
    public long getMapEntry;
    public long modifyMapEntry;

    public void add(MapReduceOperationCounter operationCounter) {
        mapReduce += operationCounter.mapReduce;
        getMapEntry += operationCounter.getMapEntry;
        modifyMapEntry += operationCounter.modifyMapEntry;
    }

    @Override
    public String toString() {
        return "MapReduceOperationCounter{"
                + "mapReduce=" + mapReduce
                + ", getMapEntry=" + getMapEntry
                + ", modifyMapEntry=" + modifyMapEntry
                + '}';
    }
}

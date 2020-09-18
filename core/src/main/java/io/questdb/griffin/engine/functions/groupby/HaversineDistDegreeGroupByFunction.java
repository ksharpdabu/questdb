/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2020 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.griffin.engine.functions.groupby;

import io.questdb.cairo.ArrayColumnTypes;
import io.questdb.cairo.ColumnType;
import io.questdb.cairo.map.MapValue;
import io.questdb.cairo.sql.Function;
import io.questdb.cairo.sql.Record;
import io.questdb.griffin.engine.functions.DoubleFunction;
import io.questdb.griffin.engine.functions.GroupByFunction;
import io.questdb.griffin.engine.functions.TernaryFunction;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.*;

public class HaversineDistDegreeGroupByFunction extends DoubleFunction implements GroupByFunction, TernaryFunction {

    private final static double EARTH_RADIUS = 6371.088;
    private final Function latDegree;
    private final Function lonDegree;
    private final Function timestamp;
    private int valueIndex;

    public HaversineDistDegreeGroupByFunction(int position, @NotNull Function latDegree, @NotNull Function lonDegree, Function timestamp) {
        super(position);
        this.latDegree = latDegree;
        this.lonDegree = lonDegree;
        this.timestamp = timestamp;
    }

    @Override
    public Function getCenter() {
        return this.lonDegree;
    }

    @Override
    public Function getRight() {
        return this.timestamp;
    }

    @Override
    public double getDouble(Record rec) {
        return rec.getDouble(this.valueIndex + 6);
    }

    @Override
    public void interpolateGap(MapValue result,
                               MapValue value1,
                               MapValue value2,
                               long gapSize) {

        //value1
        double lat1Degrees = value1.getDouble(valueIndex + 3);
        double lon1Degrees = value1.getDouble(valueIndex + 4);
        long ts1 = value1.getTimestamp(valueIndex + 5);

        //value2
        double lat2Degrees = value2.getDouble(valueIndex);
        double lon2Degrees = value2.getDouble(valueIndex + 1);
        long ts2 = value2.getTimestamp(valueIndex + 2);

        double distance = getHaversineDistanceFromDegrees(lat1Degrees, lon1Degrees, lat2Degrees, lon2Degrees, 0);
        double interpolatedGapDistance = (gapSize * distance) / (ts2 - ts1);

        result.putDouble(this.valueIndex + 6, interpolatedGapDistance);
    }

    @Override
    public void interpolateBoundary(
            MapValue value1,
            MapValue value2,
            long boundaryTimestamp,
            boolean isEndOfBoundary) {

        //value1
        double lat1Degrees = value1.getDouble(valueIndex + 3);
        double lon1Degrees = value1.getDouble(valueIndex + 4);
        long ts1 = value1.getTimestamp(valueIndex + 5);

        //value2 - the first item in this sampling interval
        double lat2Degrees = value2.getDouble(valueIndex);
        double lon2Degrees = value2.getDouble(valueIndex + 1);
        long ts2 = value2.getTimestamp(valueIndex + 2);

        double distance = getHaversineDistanceFromDegrees(lat1Degrees, lon1Degrees, lat2Degrees, lon2Degrees, 0);
        long boundaryLength;
        if (isEndOfBoundary) {
            boundaryLength = boundaryTimestamp - ts1;
        } else {
            boundaryLength = ts2 - boundaryTimestamp;
        }
        double interpolatedBoundaryDistance = (boundaryLength * distance) / (ts2 - ts1);

        MapValue result;
        if (isEndOfBoundary) {
            result = value1;
        } else {
            result = value2;
        }

        double currentDistance = result.getDouble(valueIndex + 6);
        result.putDouble(this.valueIndex + 6, currentDistance + interpolatedBoundaryDistance);
    }

    @Override
    public void computeFirst(MapValue mapValue, Record record) {
        //first item
        mapValue.putDouble(this.valueIndex, this.latDegree.getDouble(record));
        mapValue.putDouble(this.valueIndex + 1, this.lonDegree.getDouble(record));
        mapValue.putTimestamp(this.valueIndex + 2, this.timestamp.getTimestamp(record));
        //last item
        mapValue.putDouble(this.valueIndex + 3, this.latDegree.getDouble(record));
        mapValue.putDouble(this.valueIndex + 4, this.lonDegree.getDouble(record));
        mapValue.putTimestamp(this.valueIndex + 5, this.timestamp.getTimestamp(record));
        //result
        mapValue.putDouble(this.valueIndex + 6, 0);
    }

    @Override
    public void computeNext(MapValue mapValue, Record record) {
        double lat1Degrees = mapValue.getDouble(valueIndex + 3);
        double lon1Degrees = mapValue.getDouble(valueIndex + 4);
        double lat2Degrees = this.latDegree.getDouble(record);
        double lon2Degrees = this.lonDegree.getDouble(record);
        long timestamp = this.timestamp.getTimestamp(record);
        if (!Double.isNaN(lat1Degrees) && !Double.isNaN(lon1Degrees)) {
            if (!Double.isNaN(lat2Degrees) && !Double.isNaN(lon2Degrees)) {
                double currentTotalDistance = mapValue.getDouble(this.valueIndex + 6);
                double distance = getHaversineDistanceFromDegrees(lat1Degrees, lon1Degrees, lat2Degrees, lon2Degrees, currentTotalDistance);
                mapValue.putDouble(this.valueIndex + 3, lat2Degrees);
                mapValue.putDouble(this.valueIndex + 4, lon2Degrees);
                mapValue.putTimestamp(this.valueIndex + 5, timestamp);
                mapValue.putDouble(this.valueIndex + 6, distance);
            }
        } else {
            mapValue.putDouble(this.valueIndex + 3, lat2Degrees);
            mapValue.putDouble(this.valueIndex + 4, lon2Degrees);
            mapValue.putTimestamp(this.valueIndex + 5, timestamp);
        }
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    @Override
    public void pushValueTypes(ArrayColumnTypes columnTypes) {
        this.valueIndex = columnTypes.getColumnCount();
        columnTypes.add(ColumnType.DOUBLE);
        columnTypes.add(ColumnType.DOUBLE);
        columnTypes.add(ColumnType.LONG);
        columnTypes.add(ColumnType.DOUBLE);
        columnTypes.add(ColumnType.DOUBLE);
        columnTypes.add(ColumnType.LONG);
        //result
        columnTypes.add(ColumnType.DOUBLE);
    }

    @Override
    public Function getLeft() {
        return this.latDegree;
    }

    @Override
    public void setDouble(MapValue mapValue, double value) {
        mapValue.putDouble(this.valueIndex + 6, value);
    }

    @Override
    public void setNull(MapValue mapValue) {
        mapValue.putDouble(this.valueIndex, Double.NaN);
        mapValue.putDouble(this.valueIndex + 1, Double.NaN);
        mapValue.putTimestamp(this.valueIndex + 2, 0L);
        mapValue.putDouble(this.valueIndex + 3, Double.NaN);
        mapValue.putDouble(this.valueIndex + 4, Double.NaN);
        mapValue.putTimestamp(this.valueIndex + 5, 0L);
        mapValue.putDouble(this.valueIndex + 6, 0.0);
    }

    private double getHaversineDistanceFromDegrees(double lat1Degrees, double lon1Degrees, double lat2Degrees, double lon2Degrees, double currentTotalDistance) {
        double lat1 = toRad(lat1Degrees);
        double lon1 = toRad(lon1Degrees);
        double lat2 = toRad(lat2Degrees);
        double lon2 = toRad(lon2Degrees);
        return getHaversineDistanceFromRadians(currentTotalDistance, lat1, lon1, lat2, lon2);
    }

    private double getHaversineDistanceFromRadians(double currentTotal, double lat1, double lon1, double lat2, double lon2) {
        double halfLatDist = (lat2 - lat1) / 2;
        double halfLonDist = (lon2 - lon1) / 2;
        double a = sin(halfLatDist) * sin(halfLatDist) + cos(lat1) * cos(lat2) * sin(halfLonDist) * sin(halfLonDist);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));
        currentTotal += EARTH_RADIUS * c;
        return currentTotal;
    }

    private double toRad(double deg) {
        return deg * PI / 180;
    }
}
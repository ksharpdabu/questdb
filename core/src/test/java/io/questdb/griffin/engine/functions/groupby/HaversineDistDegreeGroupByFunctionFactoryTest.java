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

import io.questdb.cairo.TableWriter;
import io.questdb.cairo.sql.Record;
import io.questdb.cairo.sql.RecordCursor;
import io.questdb.cairo.sql.RecordCursorFactory;
import io.questdb.griffin.AbstractGriffinTest;
import io.questdb.griffin.SqlException;
import io.questdb.griffin.engine.functions.rnd.SharedRandom;
import io.questdb.std.Rnd;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HaversineDistDegreeGroupByFunctionFactoryTest extends AbstractGriffinTest {

    public static final double DELTA = 0.0001;

    @Before
    public void setUp3() {
        SharedRandom.RANDOM.set(new Rnd());
    }

    @Test
    public void test10Rows() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            double latDegree = -5;
            double lonDegree = -6;
            long ts = 0;
            for (int i = 0; i < 10; i++) {
                TableWriter.Row r = w.newRow();
                r.putDouble(0, latDegree);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                latDegree += 1;
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
            w.commit();
        }
        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(1414.545985354098, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void test10RowsAndNullAtEnd() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            double latDegree = -5;
            double lonDegree = -6;
            long ts = 0;
            TableWriter.Row r;
            for (int i = 0; i < 10; i++) {
                r = w.newRow();
                r.putDouble(0, latDegree);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                latDegree += 1;
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
//            r = w.newRow();
//            r.append();
            w.commit();
        }
        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(1414.545985354098, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void test2DistancesAtEquator() throws SqlException {

        compiler.compile("create table tab1 (lat double, lon double, k timestamp)", sqlExecutionContext);
        compiler.compile("create table tab2 (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab1")) {
            double lonDegree = 0;
            long ts = 0;
            for (int i = 0; i < 10; i++) {
                TableWriter.Row r = w.newRow();
                r.putDouble(0, 0);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
            w.commit();
        }

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab2")) {
            double lonDegree = -180;
            long ts = 0;
            for (int i = 0; i < 10; i++) {
                TableWriter.Row r = w.newRow();
                r.putDouble(0, 0);
                r.putDouble(1, lonDegree);
                r.append();
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
            w.commit();
        }

        double distance1;
        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab1", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                distance1 = record.getDouble(0);
            }
        }

        double distance2;
        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab2", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                distance2 = record.getDouble(0);

            }
        }
        Assert.assertEquals(distance1, distance2, DELTA);
    }

    @Test
    public void test3Rows() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            double latDegree = 1;
            double lonDegree = 2;
            long ts = 0;
            for (int i = 0; i < 3; i++) {
                TableWriter.Row r = w.newRow();
                r.putDouble(0, latDegree);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                latDegree += 1;
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
            w.commit();
        }
        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(314.4073265716869, record.getDouble(0), DELTA);
            }
        }
    }

    //"select s, haversine_dist_deg(lat, lon, k) from tab",
    @Test
    public void testAggregationBySymbol() throws SqlException {

        compiler.compile("create table tab (s symbol, lat double, lon double, k timestamp) timestamp(k) partition by NONE", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            double latDegree = -5;
            double lonDegree = -6;
            long ts = 0;
            for (int i = 0; i < 10; i++) {
                TableWriter.Row r = w.newRow();
                r.putSym(0, "AAA");
                r.putDouble(1, latDegree);
                r.putDouble(2, lonDegree);
                r.putTimestamp(3, ts);
                r.append();
                latDegree += 1;
                lonDegree += 1;
                ts += 360000000L;
            }
            w.commit();
            latDegree = -20;
            lonDegree = 10;
            for (int i = 0; i < 10; i++) {
                TableWriter.Row r = w.newRow();
                r.putSym(0, "BBB");
                r.putDouble(1, latDegree);
                r.putDouble(2, lonDegree);
                r.putTimestamp(3, ts);
                r.append();
                latDegree += 0.1;
                lonDegree += 0.1;
                ts += 360000000L;
            }
            w.commit();
        }

        try (RecordCursorFactory factory = compiler.compile("select s, haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(2, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals("AAA", record.getSym(0));
                Assert.assertEquals(1414.545985354098, record.getDouble(1), DELTA);
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals("BBB", record.getSym(0));
                Assert.assertEquals(137.51028123371657, record.getDouble(1), DELTA);
            }
        }
    }

    //select s, haversine_dist_deg(lat, lon, k), k from tab sample by 3h fill(linear)
    @Test
    public void testAggregationWithSampleFill1() throws Exception {

        assertQuery("s\tlat\tlon\tk\n" +
                        "VTJW\t-5.0\t-6.0\t1970-01-03T00:31:40.000000Z\n" +
                        "VTJW\t-4.0\t-5.0\t1970-01-03T01:03:20.000000Z\n" +
                        "VTJW\t-3.0\t-4.0\t1970-01-03T01:35:00.000000Z\n" +
                        "VTJW\t-2.0\t-3.0\t1970-01-03T02:06:40.000000Z\n" +
                        "VTJW\t-1.0\t-2.0\t1970-01-03T02:38:20.000000Z\n" +
                        "VTJW\t0.0\t-1.0\t1970-01-03T03:10:00.000000Z\n",
                "tab",
                "create table tab as " +
                        "(" +
                        "select" +
                        " rnd_symbol(1,4,4,0) s," +
                        " (-6.0 + (1*x)) lat," +
                        " (-7.0 + (1*x)) lon," +
                        " timestamp_sequence(174700000000, 1900000000) k" +
                        " from" +
                        " long_sequence(6)" +
                        ") timestamp(k) partition by NONE",
                "k",
                "insert into tab select * from (" +
                        "select" +
                        " rnd_symbol(2,4,4,0) s," +
                        " (-40.0 + (1*x)) lat," +
                        " (5.0 + (1*x)) lon," +
                        " timestamp_sequence(227200000000, 1900000000) k" +
                        " from" +
                        " long_sequence(1)" +
                        ") timestamp(k)",
                "s\tlat\tlon\tk\n" +
                        "VTJW\t-5.0\t-6.0\t1970-01-03T00:31:40.000000Z\n" +
                        "VTJW\t-4.0\t-5.0\t1970-01-03T01:03:20.000000Z\n" +
                        "VTJW\t-3.0\t-4.0\t1970-01-03T01:35:00.000000Z\n" +
                        "VTJW\t-2.0\t-3.0\t1970-01-03T02:06:40.000000Z\n" +
                        "VTJW\t-1.0\t-2.0\t1970-01-03T02:38:20.000000Z\n" +
                        "VTJW\t0.0\t-1.0\t1970-01-03T03:10:00.000000Z\n" +
                        "RXGZ\t-39.0\t6.0\t1970-01-03T15:06:40.000000Z\n",
                true, true, true);

        assertQuery("s\thaversine_dist_deg\tk\n" +
                        "AAA\t943.0302845043686\t1970-01-01T00:00:00.000000Z\n" +
                        "BBB\t786.1380286764727\t1970-01-01T00:00:00.000000Z\n" +
                        "AAA\t627.7631171110919\t1970-01-01T01:00:00.000000Z\n" +
                        "BBB\t156.39320314017536\t1970-01-01T01:00:00.000000Z\n" +
                        "AAA\t622.1211154227233\t1970-01-01T02:00:00.000000Z\n" +
                        "BBB\t155.40178053801114\t1970-01-01T02:00:00.000000Z\n",
                "select s, haversine_dist_deg(lat, lon, k), k from tab sample by 1h fill(linear)",
                null,
                "k",
                true, true, true);

    }

    @Test
    public void testAggregationWithSampleFill2_DataStartsOnTheClock() throws Exception {
        assertQuery("s\tlat\tlon\tk\n" +
                        "AAA\t-5.0\t-6.0\t1970-01-01T00:00:00.000000Z\n" +
                        "AAA\t-4.0\t-5.0\t1970-01-01T00:10:00.000000Z\n" +
                        "AAA\t-3.0\t-4.0\t1970-01-01T00:20:00.000000Z\n" +
                        "AAA\t-2.0\t-3.0\t1970-01-01T00:30:00.000000Z\n" +
                        "AAA\t-1.0\t-2.0\t1970-01-01T00:40:00.000000Z\n" +
                        "AAA\t0.0\t-1.0\t1970-01-01T00:50:00.000000Z\n" +
                        "AAA\t1.0\t0.0\t1970-01-01T01:00:00.000000Z\n" +
                        "AAA\t2.0\t1.0\t1970-01-01T01:10:00.000000Z\n" +
                        "AAA\t3.0\t2.0\t1970-01-01T01:20:00.000000Z\n" +
                        "AAA\t4.0\t3.0\t1970-01-01T01:30:00.000000Z\n" +
                        "AAA\t5.0\t4.0\t1970-01-01T01:40:00.000000Z\n" +
                        "AAA\t6.0\t5.0\t1970-01-01T01:50:00.000000Z\n" +
                        "AAA\t7.0\t6.0\t1970-01-01T02:00:00.000000Z\n" +
                        "AAA\t8.0\t7.0\t1970-01-01T02:10:00.000000Z\n" +
                        "AAA\t9.0\t8.0\t1970-01-01T02:20:00.000000Z\n" +
                        "AAA\t10.0\t9.0\t1970-01-01T02:30:00.000000Z\n" +
                        "AAA\t11.0\t10.0\t1970-01-01T02:40:00.000000Z\n" +
                        "AAA\t12.0\t11.0\t1970-01-01T02:50:00.000000Z\n" +
                        "AAA\t13.0\t12.0\t1970-01-01T03:00:00.000000Z\n" +
                        "AAA\t14.0\t13.0\t1970-01-01T03:10:00.000000Z\n"
                , "tab", "create table tab as " +
                        "(" +
                        "select" +
                        " rnd_symbol('AAA') s," +
                        " (-6.0 + (1*x)) lat," +
                        " (-7.0 + (1*x)) lon," +
                        " timestamp_sequence(0, 600000000) k" +
                        " from" +
                        " long_sequence(20)" +
                        ") timestamp(k) partition by NONE", "k", true, false, true);

        assertQuery("s\thaversine_dist_deg\tk\n" +
                        "AAA\t785.779158355717\t1970-01-01T00:00:00.000000Z\n" +
                        "AAA\t785.4205536161624\t1970-01-01T01:00:00.000000Z\n" +
                        "AAA\t780.7836318756217\t1970-01-01T02:00:00.000000Z\n" +
                        "AAA\t155.09709548701773\t1970-01-01T03:00:00.000000Z\n"
                , "select s, haversine_dist_deg(lat, lon, k), k from tab sample by 1h fill(linear)", null, "k", true, true, true);
    }

    @Test
    public void testAggregationWithSampleFill3() throws Exception {

        assertQuery("s	lat	lon	k\n" +
                        "AAA\t-5.0\t-6.0\t1970-01-01T00:00:01.000000Z\n" +
                        "AAA\t-4.0\t-5.0\t1970-01-01T00:08:21.000000Z\n" +
                        "AAA\t-3.0\t-4.0\t1970-01-01T00:16:41.000000Z\n" +
                        "AAA\t-2.0\t-3.0\t1970-01-01T00:25:01.000000Z\n" +
                        "AAA\t-1.0\t-2.0\t1970-01-01T00:33:21.000000Z\n" +
                        "AAA\t0.0\t-1.0\t1970-01-01T00:41:41.000000Z\n" +
                        "AAA\t1.0\t0.0\t1970-01-01T00:50:01.000000Z\n" +
                        "AAA\t2.0\t1.0\t1970-01-01T00:58:21.000000Z\n" +
                        "AAA\t3.0\t2.0\t1970-01-01T01:06:41.000000Z\n" +
                        "AAA\t4.0\t3.0\t1970-01-01T01:15:01.000000Z\n" +
                        "AAA\t5.0\t4.0\t1970-01-01T01:23:21.000000Z\n" +
                        "AAA\t6.0\t5.0\t1970-01-01T01:31:41.000000Z\n" +
                        "AAA\t7.0\t6.0\t1970-01-01T01:40:01.000000Z\n" +
                        "AAA\t8.0\t7.0\t1970-01-01T01:48:21.000000Z\n" +
                        "AAA\t9.0\t8.0\t1970-01-01T01:56:41.000000Z\n" +
                        "AAA\t10.0\t9.0\t1970-01-01T02:05:01.000000Z\n" +
                        "AAA\t11.0\t10.0\t1970-01-01T02:13:21.000000Z\n" +
                        "AAA\t12.0\t11.0\t1970-01-01T02:21:41.000000Z\n" +
                        "AAA\t13.0\t12.0\t1970-01-01T02:30:01.000000Z\n" +
                        "AAA\t14.0\t13.0\t1970-01-01T02:38:21.000000Z\n",
                "tab",
                "create table tab as " +
                        "(" +
                        "select" +
                        " rnd_symbol('AAA') s," +
                        " (-6.0 + (1*x)) lat," +
                        " (-7.0 + (1*x)) lon," +
                        " timestamp_sequence(1000000, 500000000) k" +
                        " from" +
                        " long_sequence(20)" +
                        ") timestamp(k) partition by NONE",
                "k",
                true, true, true);

        assertQuery("s\thaversine_dist_deg\tk\n" +
                        "AAA\t1100.2583153768578\t1970-01-01T00:00:00.000000Z\n" +
                        "AAA\t940.7395858291213\t1970-01-01T01:00:00.000000Z\n" +
                        "AAA\t622.1261899611127\t1970-01-01T02:00:00.000000Z\n",
                "select s, haversine_dist_deg(lat, lon, k), k from tab sample by 1h fill(linear)",
                null,
                "k",
                true, true, true);
    }

    @Test
    public void testAggregationWithSampleFill4() throws Exception {

        assertQuery("s\tlat\tlon\tk\n" +
                        "AAA\t-5.0\t-6.0\t1970-01-01T00:00:00.000000Z\n" +
                        "AAA\t-4.0\t-5.0\t1970-01-01T00:30:00.000000Z\n" +
                        "AAA\t-3.0\t-4.0\t1970-01-01T01:00:00.000000Z\n" +
                        "AAA\t-2.0\t-3.0\t1970-01-01T01:30:00.000000Z\n" +
                        "AAA\t-1.0\t-2.0\t1970-01-01T02:00:00.000000Z\n" +
                        "AAA\t0.0\t-1.0\t1970-01-01T02:30:00.000000Z\n" +
                        "AAA\t1.0\t0.0\t1970-01-01T03:00:00.000000Z\n" +
                        "AAA\t2.0\t1.0\t1970-01-01T03:30:00.000000Z\n",
                "tab",
                "create table tab as " +
                        "(" +
                        "select" +
                        " rnd_symbol('AAA') s," +
                        " (-6.0 + (1*x)) lat," +
                        " (-7.0 + (1*x)) lon," +
                        " timestamp_sequence(0, 1800000000) k" +
                        " from" +
                        " long_sequence(8)" +
                        ") timestamp(k) partition by NONE",
                "k",
                true, true, true);

        assertQuery("s\thaversine_dist_deg\tk\n" +
                        "AAA\t157.01233135733582\t1970-01-01T00:00:00.000000Z\n" +
                        "AAA\t157.17972284345245\t1970-01-01T01:00:00.000000Z\n" +
                        "AAA\t157.25155329290644\t1970-01-01T02:00:00.000000Z\n" +
                        "AAA\t157.22760372823444\t1970-01-01T03:00:00.000000Z\n",
                "select s, haversine_dist_deg(lat, lon, k), k from tab sample by 1h fill(linear)",
                null,
                "k",
                true, true, true);
    }

    //select s, haversine_dist_deg(lat, lon, k), k from tab sample by 3h fill(linear)
    @Test
    public void testAggregationWithSampleFill5() throws Exception {

        assertQuery("s\tlat\tlon\tk\n" +
                        "\t-5.0\t-6.0\t1970-01-03T00:00:00.000000Z\n" +
                        "\t-4.0\t-5.0\t1970-01-03T00:06:00.000000Z\n" +
                        "HYRX\t-3.0\t-4.0\t1970-01-03T00:12:00.000000Z\n" +
                        "\t-2.0\t-3.0\t1970-01-03T00:18:00.000000Z\n" +
                        "VTJW\t-1.0\t-2.0\t1970-01-03T00:24:00.000000Z\n" +
                        "VTJW\t0.0\t-1.0\t1970-01-03T00:30:00.000000Z\n" +
                        "VTJW\t1.0\t0.0\t1970-01-03T00:36:00.000000Z\n" +
                        "\t2.0\t1.0\t1970-01-03T00:42:00.000000Z\n" +
                        "RXGZ\t3.0\t2.0\t1970-01-03T00:48:00.000000Z\n" +
                        "RXGZ\t4.0\t3.0\t1970-01-03T00:54:00.000000Z\n" +
                        "\t5.0\t4.0\t1970-01-03T01:00:00.000000Z\n" +
                        "PEHN\t6.0\t5.0\t1970-01-03T01:06:00.000000Z\n" +
                        "VTJW\t7.0\t6.0\t1970-01-03T01:12:00.000000Z\n" +
                        "\t8.0\t7.0\t1970-01-03T01:18:00.000000Z\n" +
                        "\t9.0\t8.0\t1970-01-03T01:24:00.000000Z\n" +
                        "CPSW\t10.0\t9.0\t1970-01-03T01:30:00.000000Z\n" +
                        "PEHN\t11.0\t10.0\t1970-01-03T01:36:00.000000Z\n" +
                        "VTJW\t12.0\t11.0\t1970-01-03T01:42:00.000000Z\n" +
                        "HYRX\t13.0\t12.0\t1970-01-03T01:48:00.000000Z\n" +
                        "\t14.0\t13.0\t1970-01-03T01:54:00.000000Z\n" +
                        "\t15.0\t14.0\t1970-01-03T02:00:00.000000Z\n" +
                        "VTJW\t16.0\t15.0\t1970-01-03T02:06:00.000000Z\n" +
                        "PEHN\t17.0\t16.0\t1970-01-03T02:12:00.000000Z\n" +
                        "\t18.0\t17.0\t1970-01-03T02:18:00.000000Z\n" +
                        "PEHN\t19.0\t18.0\t1970-01-03T02:24:00.000000Z\n" +
                        "\t20.0\t19.0\t1970-01-03T02:30:00.000000Z\n" +
                        "CPSW\t21.0\t20.0\t1970-01-03T02:36:00.000000Z\n" +
                        "PEHN\t22.0\t21.0\t1970-01-03T02:42:00.000000Z\n" +
                        "CPSW\t23.0\t22.0\t1970-01-03T02:48:00.000000Z\n" +
                        "VTJW\t24.0\t23.0\t1970-01-03T02:54:00.000000Z\n" +
                        "VTJW\t25.0\t24.0\t1970-01-03T03:00:00.000000Z\n" +
                        "\t26.0\t25.0\t1970-01-03T03:06:00.000000Z\n" +
                        "PEHN\t27.0\t26.0\t1970-01-03T03:12:00.000000Z\n" +
                        "\t28.0\t27.0\t1970-01-03T03:18:00.000000Z\n" +
                        "\t29.0\t28.0\t1970-01-03T03:24:00.000000Z\n" +
                        "\t30.0\t29.0\t1970-01-03T03:30:00.000000Z\n" +
                        "\t31.0\t30.0\t1970-01-03T03:36:00.000000Z\n" +
                        "\t32.0\t31.0\t1970-01-03T03:42:00.000000Z\n" +
                        "\t33.0\t32.0\t1970-01-03T03:48:00.000000Z\n" +
                        "\t34.0\t33.0\t1970-01-03T03:54:00.000000Z\n" +
                        "\t35.0\t34.0\t1970-01-03T04:00:00.000000Z\n" +
                        "PEHN\t36.0\t35.0\t1970-01-03T04:06:00.000000Z\n" +
                        "RXGZ\t37.0\t36.0\t1970-01-03T04:12:00.000000Z\n" +
                        "\t38.0\t37.0\t1970-01-03T04:18:00.000000Z\n" +
                        "\t39.0\t38.0\t1970-01-03T04:24:00.000000Z\n" +
                        "\t40.0\t39.0\t1970-01-03T04:30:00.000000Z\n" +
                        "CPSW\t41.0\t40.0\t1970-01-03T04:36:00.000000Z\n" +
                        "PEHN\t42.0\t41.0\t1970-01-03T04:42:00.000000Z\n" +
                        "RXGZ\t43.0\t42.0\t1970-01-03T04:48:00.000000Z\n" +
                        "VTJW\t44.0\t43.0\t1970-01-03T04:54:00.000000Z\n" +
                        "RXGZ\t45.0\t44.0\t1970-01-03T05:00:00.000000Z\n" +
                        "\t46.0\t45.0\t1970-01-03T05:06:00.000000Z\n" +
                        "\t47.0\t46.0\t1970-01-03T05:12:00.000000Z\n" +
                        "HYRX\t48.0\t47.0\t1970-01-03T05:18:00.000000Z\n" +
                        "\t49.0\t48.0\t1970-01-03T05:24:00.000000Z\n" +
                        "\t50.0\t49.0\t1970-01-03T05:30:00.000000Z\n" +
                        "RXGZ\t51.0\t50.0\t1970-01-03T05:36:00.000000Z\n" +
                        "RXGZ\t52.0\t51.0\t1970-01-03T05:42:00.000000Z\n" +
                        "CPSW\t53.0\t52.0\t1970-01-03T05:48:00.000000Z\n" +
                        "\t54.0\t53.0\t1970-01-03T05:54:00.000000Z\n" +
                        "RXGZ\t55.0\t54.0\t1970-01-03T06:00:00.000000Z\n" +
                        "CPSW\t56.0\t55.0\t1970-01-03T06:06:00.000000Z\n" +
                        "\t57.0\t56.0\t1970-01-03T06:12:00.000000Z\n" +
                        "\t58.0\t57.0\t1970-01-03T06:18:00.000000Z\n" +
                        "\t59.0\t58.0\t1970-01-03T06:24:00.000000Z\n" +
                        "HYRX\t60.0\t59.0\t1970-01-03T06:30:00.000000Z\n" +
                        "PEHN\t61.0\t60.0\t1970-01-03T06:36:00.000000Z\n" +
                        "\t62.0\t61.0\t1970-01-03T06:42:00.000000Z\n" +
                        "\t63.0\t62.0\t1970-01-03T06:48:00.000000Z\n" +
                        "PEHN\t64.0\t63.0\t1970-01-03T06:54:00.000000Z\n" +
                        "\t65.0\t64.0\t1970-01-03T07:00:00.000000Z\n" +
                        "\t66.0\t65.0\t1970-01-03T07:06:00.000000Z\n" +
                        "VTJW\t67.0\t66.0\t1970-01-03T07:12:00.000000Z\n" +
                        "PEHN\t68.0\t67.0\t1970-01-03T07:18:00.000000Z\n" +
                        "\t69.0\t68.0\t1970-01-03T07:24:00.000000Z\n" +
                        "\t70.0\t69.0\t1970-01-03T07:30:00.000000Z\n" +
                        "CPSW\t71.0\t70.0\t1970-01-03T07:36:00.000000Z\n" +
                        "RXGZ\t72.0\t71.0\t1970-01-03T07:42:00.000000Z\n" +
                        "\t73.0\t72.0\t1970-01-03T07:48:00.000000Z\n" +
                        "HYRX\t74.0\t73.0\t1970-01-03T07:54:00.000000Z\n" +
                        "CPSW\t75.0\t74.0\t1970-01-03T08:00:00.000000Z\n" +
                        "\t76.0\t75.0\t1970-01-03T08:06:00.000000Z\n" +
                        "\t77.0\t76.0\t1970-01-03T08:12:00.000000Z\n" +
                        "\t78.0\t77.0\t1970-01-03T08:18:00.000000Z\n" +
                        "VTJW\t79.0\t78.0\t1970-01-03T08:24:00.000000Z\n" +
                        "CPSW\t80.0\t79.0\t1970-01-03T08:30:00.000000Z\n" +
                        "VTJW\t81.0\t80.0\t1970-01-03T08:36:00.000000Z\n" +
                        "\t82.0\t81.0\t1970-01-03T08:42:00.000000Z\n" +
                        "\t83.0\t82.0\t1970-01-03T08:48:00.000000Z\n" +
                        "\t84.0\t83.0\t1970-01-03T08:54:00.000000Z\n" +
                        "VTJW\t85.0\t84.0\t1970-01-03T09:00:00.000000Z\n" +
                        "\t86.0\t85.0\t1970-01-03T09:06:00.000000Z\n" +
                        "\t87.0\t86.0\t1970-01-03T09:12:00.000000Z\n" +
                        "\t88.0\t87.0\t1970-01-03T09:18:00.000000Z\n" +
                        "PEHN\t89.0\t88.0\t1970-01-03T09:24:00.000000Z\n" +
                        "HYRX\t90.0\t89.0\t1970-01-03T09:30:00.000000Z\n" +
                        "\t91.0\t90.0\t1970-01-03T09:36:00.000000Z\n" +
                        "\t92.0\t91.0\t1970-01-03T09:42:00.000000Z\n" +
                        "CPSW\t93.0\t92.0\t1970-01-03T09:48:00.000000Z\n" +
                        "\t94.0\t93.0\t1970-01-03T09:54:00.000000Z\n",
                "tab",
                "create table tab as " +
                        "(" +
                        "select" +
                        " rnd_symbol(5,4,4,1) s," +
                        " (-6.0 + (1*x)) lat," +
                        " (-7.0 + (1*x)) lon," +
                        " timestamp_sequence(172800000000, 360000000) k" +
                        " from" +
                        " long_sequence(100)" +
                        ") timestamp(k) partition by NONE",
                "k",
                "insert into tab select * from (" +
                        "select" +
                        " rnd_symbol(5,4,4,1) b," +
                        " (-40.0 + (1*x)) lat," +
                        " (5.0 + (1*x)) lon," +
                        " timestamp_sequence(277200000000, 360000000) k" +
                        " from" +
                        " long_sequence(35)" +
                        ") timestamp(k)",
                "s\tlat\tlon\tk\n" +
                        "\t-5.0\t-6.0\t1970-01-03T00:00:00.000000Z\n" +
                        "\t-4.0\t-5.0\t1970-01-03T00:06:00.000000Z\n" +
                        "HYRX\t-3.0\t-4.0\t1970-01-03T00:12:00.000000Z\n" +
                        "\t-2.0\t-3.0\t1970-01-03T00:18:00.000000Z\n" +
                        "VTJW\t-1.0\t-2.0\t1970-01-03T00:24:00.000000Z\n" +
                        "VTJW\t0.0\t-1.0\t1970-01-03T00:30:00.000000Z\n" +
                        "VTJW\t1.0\t0.0\t1970-01-03T00:36:00.000000Z\n" +
                        "\t2.0\t1.0\t1970-01-03T00:42:00.000000Z\n" +
                        "RXGZ\t3.0\t2.0\t1970-01-03T00:48:00.000000Z\n" +
                        "RXGZ\t4.0\t3.0\t1970-01-03T00:54:00.000000Z\n" +
                        "\t5.0\t4.0\t1970-01-03T01:00:00.000000Z\n" +
                        "PEHN\t6.0\t5.0\t1970-01-03T01:06:00.000000Z\n" +
                        "VTJW\t7.0\t6.0\t1970-01-03T01:12:00.000000Z\n" +
                        "\t8.0\t7.0\t1970-01-03T01:18:00.000000Z\n" +
                        "\t9.0\t8.0\t1970-01-03T01:24:00.000000Z\n" +
                        "CPSW\t10.0\t9.0\t1970-01-03T01:30:00.000000Z\n" +
                        "PEHN\t11.0\t10.0\t1970-01-03T01:36:00.000000Z\n" +
                        "VTJW\t12.0\t11.0\t1970-01-03T01:42:00.000000Z\n" +
                        "HYRX\t13.0\t12.0\t1970-01-03T01:48:00.000000Z\n" +
                        "\t14.0\t13.0\t1970-01-03T01:54:00.000000Z\n" +
                        "\t15.0\t14.0\t1970-01-03T02:00:00.000000Z\n" +
                        "VTJW\t16.0\t15.0\t1970-01-03T02:06:00.000000Z\n" +
                        "PEHN\t17.0\t16.0\t1970-01-03T02:12:00.000000Z\n" +
                        "\t18.0\t17.0\t1970-01-03T02:18:00.000000Z\n" +
                        "PEHN\t19.0\t18.0\t1970-01-03T02:24:00.000000Z\n" +
                        "\t20.0\t19.0\t1970-01-03T02:30:00.000000Z\n" +
                        "CPSW\t21.0\t20.0\t1970-01-03T02:36:00.000000Z\n" +
                        "PEHN\t22.0\t21.0\t1970-01-03T02:42:00.000000Z\n" +
                        "CPSW\t23.0\t22.0\t1970-01-03T02:48:00.000000Z\n" +
                        "VTJW\t24.0\t23.0\t1970-01-03T02:54:00.000000Z\n" +
                        "VTJW\t25.0\t24.0\t1970-01-03T03:00:00.000000Z\n" +
                        "\t26.0\t25.0\t1970-01-03T03:06:00.000000Z\n" +
                        "PEHN\t27.0\t26.0\t1970-01-03T03:12:00.000000Z\n" +
                        "\t28.0\t27.0\t1970-01-03T03:18:00.000000Z\n" +
                        "\t29.0\t28.0\t1970-01-03T03:24:00.000000Z\n" +
                        "\t30.0\t29.0\t1970-01-03T03:30:00.000000Z\n" +
                        "\t31.0\t30.0\t1970-01-03T03:36:00.000000Z\n" +
                        "\t32.0\t31.0\t1970-01-03T03:42:00.000000Z\n" +
                        "\t33.0\t32.0\t1970-01-03T03:48:00.000000Z\n" +
                        "\t34.0\t33.0\t1970-01-03T03:54:00.000000Z\n" +
                        "\t35.0\t34.0\t1970-01-03T04:00:00.000000Z\n" +
                        "PEHN\t36.0\t35.0\t1970-01-03T04:06:00.000000Z\n" +
                        "RXGZ\t37.0\t36.0\t1970-01-03T04:12:00.000000Z\n" +
                        "\t38.0\t37.0\t1970-01-03T04:18:00.000000Z\n" +
                        "\t39.0\t38.0\t1970-01-03T04:24:00.000000Z\n" +
                        "\t40.0\t39.0\t1970-01-03T04:30:00.000000Z\n" +
                        "CPSW\t41.0\t40.0\t1970-01-03T04:36:00.000000Z\n" +
                        "PEHN\t42.0\t41.0\t1970-01-03T04:42:00.000000Z\n" +
                        "RXGZ\t43.0\t42.0\t1970-01-03T04:48:00.000000Z\n" +
                        "VTJW\t44.0\t43.0\t1970-01-03T04:54:00.000000Z\n" +
                        "RXGZ\t45.0\t44.0\t1970-01-03T05:00:00.000000Z\n" +
                        "\t46.0\t45.0\t1970-01-03T05:06:00.000000Z\n" +
                        "\t47.0\t46.0\t1970-01-03T05:12:00.000000Z\n" +
                        "HYRX\t48.0\t47.0\t1970-01-03T05:18:00.000000Z\n" +
                        "\t49.0\t48.0\t1970-01-03T05:24:00.000000Z\n" +
                        "\t50.0\t49.0\t1970-01-03T05:30:00.000000Z\n" +
                        "RXGZ\t51.0\t50.0\t1970-01-03T05:36:00.000000Z\n" +
                        "RXGZ\t52.0\t51.0\t1970-01-03T05:42:00.000000Z\n" +
                        "CPSW\t53.0\t52.0\t1970-01-03T05:48:00.000000Z\n" +
                        "\t54.0\t53.0\t1970-01-03T05:54:00.000000Z\n" +
                        "RXGZ\t55.0\t54.0\t1970-01-03T06:00:00.000000Z\n" +
                        "CPSW\t56.0\t55.0\t1970-01-03T06:06:00.000000Z\n" +
                        "\t57.0\t56.0\t1970-01-03T06:12:00.000000Z\n" +
                        "\t58.0\t57.0\t1970-01-03T06:18:00.000000Z\n" +
                        "\t59.0\t58.0\t1970-01-03T06:24:00.000000Z\n" +
                        "HYRX\t60.0\t59.0\t1970-01-03T06:30:00.000000Z\n" +
                        "PEHN\t61.0\t60.0\t1970-01-03T06:36:00.000000Z\n" +
                        "\t62.0\t61.0\t1970-01-03T06:42:00.000000Z\n" +
                        "\t63.0\t62.0\t1970-01-03T06:48:00.000000Z\n" +
                        "PEHN\t64.0\t63.0\t1970-01-03T06:54:00.000000Z\n" +
                        "\t65.0\t64.0\t1970-01-03T07:00:00.000000Z\n" +
                        "\t66.0\t65.0\t1970-01-03T07:06:00.000000Z\n" +
                        "VTJW\t67.0\t66.0\t1970-01-03T07:12:00.000000Z\n" +
                        "PEHN\t68.0\t67.0\t1970-01-03T07:18:00.000000Z\n" +
                        "\t69.0\t68.0\t1970-01-03T07:24:00.000000Z\n" +
                        "\t70.0\t69.0\t1970-01-03T07:30:00.000000Z\n" +
                        "CPSW\t71.0\t70.0\t1970-01-03T07:36:00.000000Z\n" +
                        "RXGZ\t72.0\t71.0\t1970-01-03T07:42:00.000000Z\n" +
                        "\t73.0\t72.0\t1970-01-03T07:48:00.000000Z\n" +
                        "HYRX\t74.0\t73.0\t1970-01-03T07:54:00.000000Z\n" +
                        "CPSW\t75.0\t74.0\t1970-01-03T08:00:00.000000Z\n" +
                        "\t76.0\t75.0\t1970-01-03T08:06:00.000000Z\n" +
                        "\t77.0\t76.0\t1970-01-03T08:12:00.000000Z\n" +
                        "\t78.0\t77.0\t1970-01-03T08:18:00.000000Z\n" +
                        "VTJW\t79.0\t78.0\t1970-01-03T08:24:00.000000Z\n" +
                        "CPSW\t80.0\t79.0\t1970-01-03T08:30:00.000000Z\n" +
                        "VTJW\t81.0\t80.0\t1970-01-03T08:36:00.000000Z\n" +
                        "\t82.0\t81.0\t1970-01-03T08:42:00.000000Z\n" +
                        "\t83.0\t82.0\t1970-01-03T08:48:00.000000Z\n" +
                        "\t84.0\t83.0\t1970-01-03T08:54:00.000000Z\n" +
                        "VTJW\t85.0\t84.0\t1970-01-03T09:00:00.000000Z\n" +
                        "\t86.0\t85.0\t1970-01-03T09:06:00.000000Z\n" +
                        "\t87.0\t86.0\t1970-01-03T09:12:00.000000Z\n" +
                        "\t88.0\t87.0\t1970-01-03T09:18:00.000000Z\n" +
                        "PEHN\t89.0\t88.0\t1970-01-03T09:24:00.000000Z\n" +
                        "HYRX\t90.0\t89.0\t1970-01-03T09:30:00.000000Z\n" +
                        "\t91.0\t90.0\t1970-01-03T09:36:00.000000Z\n" +
                        "\t92.0\t91.0\t1970-01-03T09:42:00.000000Z\n" +
                        "CPSW\t93.0\t92.0\t1970-01-03T09:48:00.000000Z\n" +
                        "\t94.0\t93.0\t1970-01-03T09:54:00.000000Z\n" +
                        "\t-39.0\t6.0\t1970-01-04T05:00:00.000000Z\n" +
                        "SUQS\t-38.0\t7.0\t1970-01-04T05:06:00.000000Z\n" +
                        "OJIP\t-37.0\t8.0\t1970-01-04T05:12:00.000000Z\n" +
                        "SUQS\t-36.0\t9.0\t1970-01-04T05:18:00.000000Z\n" +
                        "\t-35.0\t10.0\t1970-01-04T05:24:00.000000Z\n" +
                        "\t-34.0\t11.0\t1970-01-04T05:30:00.000000Z\n" +
                        "RLTK\t-33.0\t12.0\t1970-01-04T05:36:00.000000Z\n" +
                        "\t-32.0\t13.0\t1970-01-04T05:42:00.000000Z\n" +
                        "SUQS\t-31.0\t14.0\t1970-01-04T05:48:00.000000Z\n" +
                        "RLTK\t-30.0\t15.0\t1970-01-04T05:54:00.000000Z\n" +
                        "SUQS\t-29.0\t16.0\t1970-01-04T06:00:00.000000Z\n" +
                        "OJIP\t-28.0\t17.0\t1970-01-04T06:06:00.000000Z\n" +
                        "\t-27.0\t18.0\t1970-01-04T06:12:00.000000Z\n" +
                        "\t-26.0\t19.0\t1970-01-04T06:18:00.000000Z\n" +
                        "\t-25.0\t20.0\t1970-01-04T06:24:00.000000Z\n" +
                        "VVSJ\t-24.0\t21.0\t1970-01-04T06:30:00.000000Z\n" +
                        "HZEP\t-23.0\t22.0\t1970-01-04T06:36:00.000000Z\n" +
                        "RLTK\t-22.0\t23.0\t1970-01-04T06:42:00.000000Z\n" +
                        "\t-21.0\t24.0\t1970-01-04T06:48:00.000000Z\n" +
                        "\t-20.0\t25.0\t1970-01-04T06:54:00.000000Z\n" +
                        "\t-19.0\t26.0\t1970-01-04T07:00:00.000000Z\n" +
                        "\t-18.0\t27.0\t1970-01-04T07:06:00.000000Z\n" +
                        "HZEP\t-17.0\t28.0\t1970-01-04T07:12:00.000000Z\n" +
                        "\t-16.0\t29.0\t1970-01-04T07:18:00.000000Z\n" +
                        "\t-15.0\t30.0\t1970-01-04T07:24:00.000000Z\n" +
                        "HZEP\t-14.0\t31.0\t1970-01-04T07:30:00.000000Z\n" +
                        "\t-13.0\t32.0\t1970-01-04T07:36:00.000000Z\n" +
                        "RLTK\t-12.0\t33.0\t1970-01-04T07:42:00.000000Z\n" +
                        "\t-11.0\t34.0\t1970-01-04T07:48:00.000000Z\n" +
                        "HZEP\t-10.0\t35.0\t1970-01-04T07:54:00.000000Z\n" +
                        "\t-9.0\t36.0\t1970-01-04T08:00:00.000000Z\n" +
                        "\t-8.0\t37.0\t1970-01-04T08:06:00.000000Z\n" +
                        "\t-7.0\t38.0\t1970-01-04T08:12:00.000000Z\n" +
                        "\t-6.0\t39.0\t1970-01-04T08:18:00.000000Z\n" +
                        "\t-5.0\t40.0\t1970-01-04T08:24:00.000000Z\n",
                true, true, true);

        assertQuery("s\thaversine_dist_deg\tk\n" +
                        "AAA\t943.0302845043686\t1970-01-01T00:00:00.000000Z\n" +
                        "BBB\t786.1380286764727\t1970-01-01T00:00:00.000000Z\n" +
                        "AAA\t627.7631171110919\t1970-01-01T01:00:00.000000Z\n" +
                        "BBB\t156.39320314017536\t1970-01-01T01:00:00.000000Z\n" +
                        "AAA\t622.1211154227233\t1970-01-01T02:00:00.000000Z\n" +
                        "BBB\t155.40178053801114\t1970-01-01T02:00:00.000000Z\n",
                "select s, haversine_dist_deg(lat, lon, k), k from tab sample by 1h fill(linear)",
                null,
                "k",
                true, true, true);

    }

    @Test
    public void testAllNull() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            for (int i = 0; i < 2; i++) {
                TableWriter.Row r = w.newRow();
                r.append();
            }
            w.commit();
        }

        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(0, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void testCircumferenceAtEquator() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            double lonDegree = -180;
            long ts = 0;
            for (int i = 0; i < 360; i++) {
                TableWriter.Row r = w.newRow();
                r.putDouble(0, 0);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
            w.commit();
        }
        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(39919.53004981382, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void testNegativeLatLon() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            double latDegree = -1;
            double lonDegree = -2;
            long ts = 0;
            for (int i = 0; i < 2; i++) {
                TableWriter.Row r = w.newRow();
                r.putDouble(0, latDegree);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                latDegree -= 1;
                lonDegree -= 1;
            }
            w.commit();
        }
        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(157.22760372823444, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void testOneNullAtEnd() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            TableWriter.Row r;
            double latDegree = 1;
            double lonDegree = 2;
            long ts = 0;
            for (int i = 0; i < 2; i++) {
                r = w.newRow();
                r.putDouble(0, latDegree);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                latDegree += 1;
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
            r = w.newRow();
            r.append();
            w.commit();
        }

        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(157.22760372823444, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void testOneNullAtTop() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            TableWriter.Row r = w.newRow();
            r.append();
            double latDegree = 1;
            double lonDegree = 2;
            long ts = 0;
            for (int i = 0; i < 2; i++) {
                r = w.newRow();
                r.putDouble(0, latDegree);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                latDegree += 1;
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
            w.commit();
        }

        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(157.22760372823444, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void testOneNullInMiddle() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            TableWriter.Row r = w.newRow();
            r.putDouble(0, 1);
            r.putDouble(1, 2);
            r.putTimestamp(2, 10_000_000_000L);
            r.append();
            r = w.newRow();
            r.append();
            r = w.newRow();
            r.putDouble(0, 2);
            r.putDouble(1, 3);
            r.putTimestamp(2, 20_000_000_000L);
            r.append();
            w.commit();
        }

        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(157.22760372823444, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void testOneNullsInMiddle() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            TableWriter.Row r = w.newRow();
            r.putDouble(0, 1);
            r.putDouble(1, 2);
            r.putTimestamp(2, 10_000_000_000L);
            r.append();
            r = w.newRow();
            r.append();
            r = w.newRow();
            r.append();
            r = w.newRow();
            r.append();
            r = w.newRow();
            r.putDouble(0, 2);
            r.putDouble(1, 3);
            r.putTimestamp(2, 20_000_000_000L);
            r.append();
            w.commit();
        }

        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(157.22760372823444, record.getDouble(0), DELTA);
            }
        }
    }

    @Test
    public void testPositiveLatLon() throws SqlException {

        compiler.compile("create table tab (lat double, lon double, k timestamp)", sqlExecutionContext);

        try (TableWriter w = engine.getWriter(sqlExecutionContext.getCairoSecurityContext(), "tab")) {
            double latDegree = 1;
            double lonDegree = 2;
            long ts = 0;
            for (int i = 0; i < 2; i++) {
                TableWriter.Row r = w.newRow();
                r.putDouble(0, latDegree);
                r.putDouble(1, lonDegree);
                r.putTimestamp(2, ts);
                r.append();
                latDegree += 1;
                lonDegree += 1;
                ts += 10_000_000_000L;
            }
            w.commit();
        }
        try (RecordCursorFactory factory = compiler.compile("select haversine_dist_deg(lat, lon, k) from tab", sqlExecutionContext).getRecordCursorFactory()) {
            try (RecordCursor cursor = factory.getCursor(sqlExecutionContext)) {
                Record record = cursor.getRecord();
                Assert.assertEquals(1, cursor.size());
                Assert.assertTrue(cursor.hasNext());
                Assert.assertEquals(157.22760372823444, record.getDouble(0), DELTA);
            }
        }
    }
}
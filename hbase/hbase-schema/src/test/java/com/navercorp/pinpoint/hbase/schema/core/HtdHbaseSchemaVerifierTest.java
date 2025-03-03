/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.hbase.schema.core;

import com.navercorp.pinpoint.common.util.CollectionUtils;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author HyunGil Jeong
 */
public class HtdHbaseSchemaVerifierTest {

    private final HbaseSchemaVerifier<HTableDescriptor> verifier = new HtdHbaseSchemaVerifier();

    @Test
    public void emptyExpectedSchemas_shouldReturnTrue() {
        List<HTableDescriptor> emptyExpectedSchemas = Collections.emptyList();
        List<HTableDescriptor> actualSchemas = List.of(createHtd("table1", "table1_1"));
        assertThat(verifier.verifySchemas(null, actualSchemas)).isTrue();
        assertThat(verifier.verifySchemas(emptyExpectedSchemas, actualSchemas)).isTrue();
    }

    @Test
    public void emptyActualSchemas_shouldReturnFalse() {
        List<HTableDescriptor> emptyActualSchemas = Collections.emptyList();
        List<HTableDescriptor> expectedSchemas = List.of(createHtd("table1", "table1_1"));
        assertThat(verifier.verifySchemas(expectedSchemas, null)).isFalse();
        assertThat(verifier.verifySchemas(expectedSchemas, emptyActualSchemas)).isFalse();
    }

    @Test
    public void exactMatch_shouldReturnTrue() {
        List<HTableDescriptor> expectedSchemas = List.of(
                createHtd("table1", "table1_1"),
                createHtd("table2", "table2_1", "table2_2", "table2_3"),
                createHtd("table3"));
        List<HTableDescriptor> actualSchemas = copySchema(expectedSchemas);
        assertThat(verifier.verifySchemas(expectedSchemas, actualSchemas)).isTrue();
    }

    @Test
    public void excessiveTableNameMatch_shouldReturnTrue() {
        List<HTableDescriptor> expectedSchemas = List.of(
                createHtd("table1", "table1_1"),
                createHtd("table2", "table2_1", "table2_2", "table2_3"),
                createHtd("table3"));
        List<HTableDescriptor> actualSchemas = copySchema(expectedSchemas);
        actualSchemas.add(createHtd("table4", "table4_1"));
        assertThat(verifier.verifySchemas(expectedSchemas, actualSchemas)).isTrue();
    }

    @Test
    public void excessiveColumnFamilyMatch_shouldReturnTrue() {
        List<HTableDescriptor> expectedSchemas = List.of(
                createHtd("table1", "table1_1"),
                createHtd("table2", "table2_1", "table2_2", "table2_3"),
                createHtd("table3"));
        List<HTableDescriptor> actualSchemas = copySchema(expectedSchemas);
        for (HTableDescriptor htd : actualSchemas) {
            htd.addFamily(new HColumnDescriptor("newCF"));
        }
        assertThat(verifier.verifySchemas(expectedSchemas, actualSchemas)).isTrue();
    }

    @Test
    public void partialTableNameMatch_shouldReturnFalse() {
        List<HTableDescriptor> actualSchemas = List.of(
                createHtd("table1", "table1_1"),
                createHtd("table2", "table2_1", "table2_2", "table2_3"),
                createHtd("table3"));
        List<HTableDescriptor> expectedSchemas = copySchema(actualSchemas);
        expectedSchemas.add(createHtd("table4", "table4_1"));
        assertThat(verifier.verifySchemas(expectedSchemas, actualSchemas)).isFalse();
    }

    @Test
    public void partialColumnFamilyMatch_shouldReturnFalse() {
        List<HTableDescriptor> actualSchemas = List.of(
                createHtd("table1", "table1_1"),
                createHtd("table2", "table2_1", "table2_2", "table2_3"),
                createHtd("table3"));
        List<HTableDescriptor> expectedSchemas = copySchema(actualSchemas);
        for (HTableDescriptor htd : expectedSchemas) {
            htd.addFamily(new HColumnDescriptor("newCF"));
        }
        assertThat(verifier.verifySchemas(expectedSchemas, actualSchemas)).isFalse();
    }

    @Test
    public void tableNameMismatch_shouldReturnFalse() {
        List<HTableDescriptor> expectedSchemas = List.of(createHtd("table1", "CF1"));
        List<HTableDescriptor> actualSchemas = List.of(createHtd("table2", "CF1"));
        assertThat(verifier.verifySchemas(expectedSchemas, actualSchemas)).isFalse();
    }

    @Test
    public void columnFamilyMismatch_shouldReturnFalse() {
        List<HTableDescriptor> expectedSchemas = List.of(createHtd("table1", "CF1"));
        List<HTableDescriptor> actualSchemas = List.of(createHtd("table1", "CF2"));
        assertThat(verifier.verifySchemas(expectedSchemas, actualSchemas)).isFalse();
    }

    private HTableDescriptor createHtd(String tableQualifier, String... columnFamilyNames) {
        TableName tableName = TableName.valueOf(NamespaceDescriptor.DEFAULT_NAMESPACE_NAME_STR, tableQualifier);
        HTableDescriptor htd = new HTableDescriptor(tableName);
        for (String columnFamilyName : columnFamilyNames) {
            htd.addFamily(new HColumnDescriptor(columnFamilyName));
        }
        return htd;
    }

    private List<HTableDescriptor> copySchema(List<HTableDescriptor> htds) {
        if (CollectionUtils.isEmpty(htds)) {
            return Collections.emptyList();
        }
        return htds.stream().map(HTableDescriptor::new).collect(Collectors.toList());
    }
}

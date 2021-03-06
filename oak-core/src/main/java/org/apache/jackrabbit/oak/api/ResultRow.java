/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.api;


/**
 * A query result row.
 */
public interface ResultRow {
    
    /**
     * The path, assuming there is only one selector.
     * 
     * @return the path
     * @throws IllegalArgumentException if there are multiple selectors
     */
    String getPath();

    /**
     * The path for the given selector name.
     * 
     * @param selectorName the selector name (null if there is only one selector)
     * @return the path
     * @throws IllegalArgumentException if the selector was not found,
     *      or if there are multiple selectors but the passed selectorName is null
     */
    String getPath(String selectorName);

    /**
     * The property value.
     * 
     * @param columnName the column name
     * @return the value
     * @throws IllegalArgumentException if the column was not found
     */
    PropertyValue getValue(String columnName);

    /**
     * Get the list of values.
     * 
     * @return the values
     */
    PropertyValue[] getValues();

}

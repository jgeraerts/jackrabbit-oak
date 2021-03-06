/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.mongomk.blob.gridfs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

/**
 * {@code Command} for {@code MongoMicroKernel#write(InputStream)}
 */
public class WriteBlobCommandGridFS extends BaseCommand<String> {

    private final GridFS gridFS;
    private final InputStream is;

    /**
     * Constructs a {@code WriteBlobCommandMongo}
     *
     * @param gridFS GridFS instance.
     * @param is Input stream.
     */
    public WriteBlobCommandGridFS(GridFS gridFS, InputStream is) {
        super();
        this.gridFS = gridFS;
        this.is = is;
    }

    @Override
    public String execute() throws Exception {
        return saveBlob();
    }

    private String saveBlob() throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        String md5 = calculateMd5(bis);
        GridFSDBFile gridFile = gridFS.findOne(new BasicDBObject("md5", md5));
        if (gridFile != null) {
            is.close();
            return md5;
        }

        GridFSInputFile gridFSInputFile = gridFS.createFile(bis, true);
        gridFSInputFile.save();
        return gridFSInputFile.getMD5();
    }

    private static String calculateMd5(BufferedInputStream bis) throws IOException {
        bis.mark(Integer.MAX_VALUE);
        String md5 = DigestUtils.md5Hex(bis);
        bis.reset();
        return md5;
    }
}

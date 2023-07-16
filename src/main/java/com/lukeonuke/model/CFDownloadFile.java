package com.lukeonuke.model;

import java.time.Instant;
import java.util.Date;

public class CFDownloadFile {
    public class CFDownloadFileData{
        public int id;
        public int gameId;
        public int modId;
        public boolean isAvailable;
        public String displayName;
        public String fileName;
        public int releaseType;
        public int fileStatus;
        public Date fileDate;
        public int downloadCount;
        public int fileLength;
        public String downloadUrl;
    }
    public CFDownloadFileData data;
}

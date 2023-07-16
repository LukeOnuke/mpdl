package com.lukeonuke.model;

public class CFProject {
    public class CFProjectLinks{
        public String websiteUrl;
        public String wikiUrl;
        public String issuesUrl;
        public String sourceUrl;
    }

    public class CFProjectData{
        public int id;
        public int gameId;
        public String slug;
        public String name;
        public CFProjectLinks links;
    }
    public CFProjectData data;
}

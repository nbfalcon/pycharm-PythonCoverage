package org.nbfalcon.pycharmCoverage.coveragePy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

// The json output does not contain branch information
public class CoveragePyLoaderXML {
    public static CoverageOutput loadFromXML(Reader input) throws IOException {
        XmlMapper mapper = new XmlMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper.readValue(input, CoverageOutput.class);
    }

    public static class Line {
        @JacksonXmlProperty(isAttribute = true)
        public int number;

        @JacksonXmlProperty(isAttribute = true)
        public int hits;

        @JacksonXmlProperty(isAttribute = true)
        public boolean branch;

        @JacksonXmlProperty(isAttribute = true)
        public String conditionCoverage;

        @JacksonXmlProperty(isAttribute = true)
        public String missingBranches;
    }

    public static class ClassCoverage {
        @JacksonXmlProperty(isAttribute = true)
        public String name;

        @JacksonXmlElementWrapper(localName = "lines")
        @JacksonXmlProperty(localName = "lines")
        public List<Line> lines;
    }

    public static class PackageData {
        @JacksonXmlProperty(isAttribute = true)
        public String name;

        @JacksonXmlElementWrapper(localName = "classes")
        @JacksonXmlProperty(localName = "class")
        public List<ClassCoverage> classes;
    }

    @JacksonXmlRootElement(localName = "coverage")
    public static class CoverageOutput {
        @JacksonXmlElementWrapper(localName = "sources")
        @JacksonXmlProperty(localName = "source")
        public List<String> sources;

        @JacksonXmlElementWrapper(localName = "packages")
        @JacksonXmlProperty(localName = "package")
        public List<PackageData> packages;
    }
}

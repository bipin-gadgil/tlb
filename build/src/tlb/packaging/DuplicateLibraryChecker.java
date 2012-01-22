package tlb.packaging;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.resources.FileResource;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuplicateLibraryChecker extends Task {

    public static final Pattern NAME_HYPHEN_VERSION_WITH_DOTS = Pattern.compile("^(.+?)-(\\d+(\\.\\d+)*)\\.jar$");
    private List<FileSet> fileSets;
    private FileSet libDirFileset;
    private Project project;

    public DuplicateLibraryChecker() {
        fileSets = new ArrayList<FileSet>();
    }

    @Override
    public void execute() throws BuildException {
        HashMap<String, String> libNameVersionMap = new HashMap<String, String>();
        Iterator iterator = libDirFileset.iterator();
        while (iterator.hasNext()) {
            FileResource next = (FileResource) iterator.next();
            String libFileName = next.getFile().getName();
            Matcher matcher = NAME_HYPHEN_VERSION_WITH_DOTS.matcher(libFileName);
            if (matcher.matches()) {
                String baseName = matcher.group(1);
                String libVersion = matcher.group(2);
                if (libNameVersionMap.containsKey(baseName)) {
                    throw new BuildException(String.format("Duplicate library found: '%s' has atleast 2 versions, namely '%s' and '%s'.", baseName, libNameVersionMap.get(baseName), libVersion));
                } else {
                    libNameVersionMap.put(baseName, libVersion);
                }
            } else {
                throw new BuildException(String.format("Unknown lib-name format: %s doesn't match expected library-file name patterns.", libFileName));
            }
        }
    }

    public void setLibDir(String libDir) {
        libDirFileset = new FileSet();
        libDirFileset.setDir(new File(libDir));
        libDirFileset.setProject(getProject());
    }
}

import com.github.javaparser.ast.CompilationUnit;

import java.io.File;

/**
 * Written by Kevin Pagkatipunan (8/1/19)
 * <p>
 * Description: This class represents an abstract syntax tree resulting from parsing the accompanying file. This class
 * serves as a container for an abstract syntax tree and its parent file
 */


public class AbstractSyntaxTree {

    private File file;
    private CompilationUnit compUnit;//this is the abstract syntax tree

    public AbstractSyntaxTree(File file, CompilationUnit compUnit) {
        this.file = file;
        this.compUnit = compUnit;
    }

    public File getFile() {
        return this.file;
    }

    public CompilationUnit getAbstractSyntaxTree() {
        return this.compUnit;
    }

    public String toString() {
        return this.getFile().getName();
    }

}

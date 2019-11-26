import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Written by Kevin Pagkatipunan (7/28/19)
 *
 * Description: This class will find all relevant files to add annotations to.
 */

public class FilesToAnnotateFinder {

    private List<File> files; // the files to look thru to find RESTful services or DTO files
    private Set<AbstractSyntaxTree> RESTServices; // REST service files to add annotations to

    /**
     * Description: Default constructor for the FilesToAnnotate Finder
     *
     * @param files the files to search for files annotate
     */
    public FilesToAnnotateFinder(ArrayList<File> files) {
        this.files = files;
        this.RESTServices = new HashSet<>();
    }//default constructor

    /**
     * Description: Finds all files that are REST services. This method will search for the @PATH() annotation in the
     * file. If @PATH() is present, then the file is determined to be a REST service file. Otherwise, it is ignored.
     *
     * @throws IOException
     */
    public void findRESTServices(LexicalPreservingPrinter lex) throws IOException {
        JavaParser parser = new JavaParser();


        for (File currFile : files) {//for each file, find all @Path annotations
            ParseResult<CompilationUnit> temp = parser.parse(currFile);
            if (temp.isSuccessful()) {
                Optional<CompilationUnit> opt = temp.getResult();
                if (opt.isPresent()) {
                    CompilationUnit compUnit = opt.get();
                    lex.setup(compUnit);//preparing the node to be used in the lex print method
                    String name = currFile.getName();
                    name = FilenameUtils.removeExtension(name);
                    Optional<ClassOrInterfaceDeclaration> optional = compUnit.getClassByName(name);
                    if (optional.isPresent()) {
                        ClassOrInterfaceDeclaration decl = optional.get();
                        List<AnnotationExpr> ann = decl.getAnnotations();
                        String currName = currFile.getName();
                        for (AnnotationExpr elem : ann) {
                            if (elem.getName().asString().equals("Path") || currName.endsWith("Resource.java")) {//adding REST service
                                this.RESTServices.add(new AbstractSyntaxTree(currFile, compUnit));
                            }
                        }

                        if (currName.endsWith("Resource.java")) {//if a REST resource file has no annotations or @Path annotation
                            this.RESTServices.add(new AbstractSyntaxTree(currFile, compUnit));
                        }
                    }

                }
            }
        }

    }


    /**
     * Description: Iterates through the child nodes of the target node, finding all the method declarations inside
     *
     * @param compUnit the CompilationUnit to search for method declarations
     * @return a list of method declarations
     */
    public List<MethodDeclaration> getMethodDeclarations(CompilationUnit compUnit) {
        List<MethodDeclaration> methods = new ArrayList<>();
        List<Node> children = compUnit.getChildNodes();
        List<Node> childrenOfClassDecl = new ArrayList<>();
        for (Node node : children) {//finding class declaration in the files child nodes
            if (ClassOrInterfaceDeclaration.class.isAssignableFrom(node.getClass())) {//current node is a classDeclaration
                childrenOfClassDecl = node.getChildNodes();
                break;
            }
        }

        for (Node currNode : childrenOfClassDecl) {//finding methods from class declaration
            if (MethodDeclaration.class.isAssignableFrom(currNode.getClass())) {//currNode is a MethodDeclaration
                methods.add((MethodDeclaration) currNode);
            }
        }
        return methods;
    }

    static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    /**
     * Description: Creates a string to set as the @Schema description of the return type of the getter method. (For use
     * by the annotateDTOFiles method)
     *
     * @param methodNameArray
     * @return string to set as the @Schema description
     */
    public String createSchemaDescription(String[] methodNameArray) {
        String description = "The ";
        List<String> methodNameList = new ArrayList<>(Arrays.asList(methodNameArray));
        methodNameList.remove(0);//removing the "get" from the method name
        methodNameList.remove(methodNameList.size() - 1); //removing the "()" from the method name
        for (String word : methodNameList) {
            description += word + " ";
        }//appending the words together
        description = description.trim();
        return description;
    }




    public String getMethodName(MethodDeclaration method){
        String[] methodDecl = method.getDeclarationAsString(true, true, true).split(" ");//splitting the method declaration into words
        String methodName = methodDecl[2];//retrieving the method name
        return methodName;
    }

    /**
     * Description: Returns a string representing the value to add to the given label (either a description, type, or
     * implementation)
     *
     * @param label the label to generate a value for
     * @return a String representing the value of the given label
     */
    public String addLabelValue(SchemaLabel.Label label, MethodDeclaration method) {
        String[] methodDecl = method.getDeclarationAsString(true, true, true).split(" ");
        switch (label){
            case DESCRIPTION:

                String methodName = methodDecl[2];
                String methodNameSplit = splitCamelCase(methodName);
                String[] methodNameArray = methodNameSplit.split(" ");
                String description = createSchemaDescription(methodNameArray);
                return description;
            case TYPE:
                return methodDecl[1];
            case IMPLEMENTATION:
                return  methodDecl[1] + ".class";
            default:
                return "TODO: Add in description here.";
        }
    }


    /**
     * Description: Retrieves all DTO objects present in a method declaration. Checks the return type and the parameter
     * list. Returns them in a set
     * @param methodDecl a string array of all the modifiers/words in the method declaration
     * @return set of all DTO objects present in the method declaration
     */
    public Set<String> createListOfDTOsPresentInMethod(String[] methodDecl) {
        List<String> words = Arrays.asList(methodDecl);
        Set<String> results = new HashSet<>();
        Pattern pattern = Pattern.compile("([A-Z]{1}[A-Za-z]*DTO)");

        for (String word : words) {
            Matcher matcher = pattern.matcher(word);
            while (matcher.find()) {//while a match is found (keep finding DTO objects)
                String result = matcher.group();
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Description: Finds all instance variables in a file's abstract syntax tree
     *
     * @param compUnit the file's abstract syntax tree
     * @return list of the file's instance variables
     */
    public List<FieldDeclaration> getInstanceVariables(CompilationUnit compUnit) {
        List<FieldDeclaration> results = new ArrayList<>();
        List<Node> children = compUnit.getChildNodes();
        List<Node> classChildren = null;
        ClassOrInterfaceDeclaration target = null;

        for(Node node: children){//finding the class declaration
            if(ClassOrInterfaceDeclaration.class.isAssignableFrom(node.getClass())){//current node is a class declaration
              target = (ClassOrInterfaceDeclaration) node;
              classChildren = target.getChildNodes();
              break;
            }
        }

        for(Node node: classChildren){//iterating thru the children of the class declaration to find FieldDeclarations
            if(FieldDeclaration.class.isAssignableFrom(node.getClass())){//current node is an instance variable
                FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
                results.add(fieldDeclaration);
            }
        }

        return results;
    }


    /**
     * Description: Checks to see if the given string can be converted into a SchemaLabel.label. If so return true.
     * Otherwise return false. (for use by the getSchemaLabels() method)
     * @param test
     * @return true if the given string can be converted into a SchemaLabel.Label, false otherwise
     */
    public static boolean isASchemaLabel(String test) {

        for (SchemaLabel.Label c : SchemaLabel.Label.values()) {//iterating thru the list of labels to check for
            if (c.name().equals(test)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Description: Returns a list of schema labels that are present in the list of node children (for use by the
     * needsRequiredSchemaLabels method)
     *
     * @param children the schema annotation's children nodes or content
     * @return list of schema labels present in the children nodes
     */
    public List<SchemaLabel.Label> getSchemaLabels(List<Node> children){
        List<SchemaLabel.Label> labels = new ArrayList<>();

        for(Node node: children){
            if(MemberValuePair.class.isAssignableFrom(node.getClass())){//current node is a member value pair
                MemberValuePair pair = (MemberValuePair) node;
                String labelName = pair.getNameAsString();

                if(isASchemaLabel(labelName)) {//the current schema label can be converted into a SchemaLabel.Label
                    // (This class represents all schema labels to add to annotation, all others will be ignored)
                    SchemaLabel.Label label = SchemaLabel.Label.valueOf(labelName.toUpperCase());
                    labels.add(label);
                }
            }
        }
        return labels;
    }


    /**
     * Description: Returns a list of Schema labels to add to the existing Schema annotation. An empty list will be
     * returned if the schema annotation contains all required labels or if a schema annotation does not exist in the
     * given method. Currently, it is expected that a DTO object has #description, #type, and #implementation labels.
     * A non DTO object will have only #description, and #type labels
     * @param method the method to check for Schema labels
     * @return boolean value representing whether the required labels must be added
     */
    public List<SchemaLabel.Label> needsRequiredSchemaLabels(MethodDeclaration method) {
        List<SchemaLabel.Label> results = new ArrayList<>();
        if (method.isAnnotationPresent(Schema.class)) {//schema annotation is present, so check if it needs more labels
            String methodDecl = method.getDeclarationAsString(true, true, true);
            String[] methodDeclToWords = methodDecl.split(" ");//splitting the method declaration into words
            String returnType = methodDeclToWords[1];//retrieving the method return type
            Optional<AnnotationExpr> opt = method.getAnnotationByClass(Schema.class);
            if (opt.isPresent()) {
                AnnotationExpr schema = opt.get();
                List<Node> children = schema.getChildNodes();
                List<SchemaLabel.Label> labelsPresent = getSchemaLabels(children);
                if (returnType.toUpperCase().endsWith("DTO")) {//the return type of this method is a DTO
                    List<SchemaLabel.Label> labelsToCheckFor = new SchemaLabel().getLabelsDTOShouldHave();
                    for (SchemaLabel.Label label : labelsToCheckFor) {
                        if (!labelsPresent.contains(label)) {//checking if the existing labels contain all the required labels
                            results.add(label);//adding to label to add to existing Schema annotation
                        }
                    }
                } else {//the return type of the method is not a DTO
                    if ((!labelsPresent.contains("description")) || (!labelsPresent.contains("type"))) {
                        List<SchemaLabel.Label> labelsToCheckFor = new SchemaLabel().getLabelsNonDTOShouldHave();
                        for (SchemaLabel.Label label : labelsToCheckFor) {
                            if (!labelsPresent.contains(label)) {//checking if the existing labels contain all the required labels
                                results.add(label);//adding to label to add to existing Schema annotation
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Description: Annotates the getter method with swagger schema annotations. If a schema annotation
     * already exists in the current method, then a check is made to ensure that all the required schema labels are present
     * in the existing schema annotation. If not then the missing labels are added. This method also finds all DTO objects
     * present in the methods and returns them in a set (for use by the annotateGetters() method)
     *
     * @param methods the list of methods to annotate
     * @return set of DTO objects present in the getter methods of the file
     */
    public Set<String> annotateGetters(List<MethodDeclaration> methods) {
        Set<String> toTraverse = new HashSet<>();

        for (MethodDeclaration method : methods) {
            //need to get the getters
            String methodName = getMethodName(method);
            List<String> existingLabels = new ArrayList<>();
            String[] methodDeclToWords = method.getDeclarationAsString(true, true, true).split(" ");
            if (methodName.toLowerCase().startsWith("get")) {//method is a getter so annotate it
                if (method.isAnnotationPresent(Schema.class)) {//Schema annotation is present, check if more labels need to be added
                    List<SchemaLabel.Label> labelsToAdd = needsRequiredSchemaLabels(method);
                    if (labelsToAdd.size() != 0) {
                        Optional<AnnotationExpr> opt = method.getAnnotationByClass(Schema.class);
                        if (opt.isPresent()) {
                            AnnotationExpr ann = opt.get();
                            List<Node> children = ann.getChildNodes();
                            for (Node child : children) {//getting the names of the labels that already exist in the @schema annotation
                                if (MemberValuePair.class.isAssignableFrom(child.getClass())) {
                                    MemberValuePair pair = (MemberValuePair) child;
                                    existingLabels.add(pair.getNameAsString());
                                }
                            }
                            NormalAnnotationExpr normalAnnotationExpr = (NormalAnnotationExpr) ann;
                            for (SchemaLabel.Label label : labelsToAdd) {//iterating thru labels to add
                                if (!existingLabels.contains(label.toString().toLowerCase())) {//the current label to add does not already exist in the schema annotation
                                    if (label.equals(SchemaLabel.Label.IMPLEMENTATION)) {
                                        normalAnnotationExpr.addPair(label.toString().toLowerCase(), addLabelValue(label, method));
                                    } else {
                                        normalAnnotationExpr.addPair(label.toString().toLowerCase(), "\"" + addLabelValue(label, method) + "\"");
                                    }
                                }
                            }
                        }
                    }
                } else {//Schema annotation is not present, so add it
                    NodeWithAnnotations annotatedNode = method;
                    NormalAnnotationExpr ann = annotatedNode.addAndGetAnnotation(Schema.class);
                    String[] methodNameToWordsArray = splitCamelCase(methodName).split(" ");//creating an array from the method name
                    String description = createSchemaDescription(methodNameToWordsArray);
                    ann.addPair("description", "\"" + description + "\"");
                    ann.addPair("type", "\"" + methodDeclToWords[1] + "\"");//adding the return type of the getter method
                }
            }//end annotation process

            toTraverse.addAll(createListOfDTOsPresentInMethod(methodDeclToWords));//adding all DTO objects to traverse
        }
        return toTraverse;
    }

    /**
     * Description: Retrieves any DTO objects that exist in the list of instance variables.
     * (for use by the annotateDTOfiles method)
     *
     * @param instanceVariables
     * @return a set of DTO files present in the instance variables
     */
    public Set<String> getDTOsFromInstanceVariables(List<FieldDeclaration> instanceVariables) {
        Set<String> results = new HashSet<>();
        Pattern pattern = Pattern.compile("^([A-Z][A-Za-z]*DTO)+");

        for (FieldDeclaration field : instanceVariables) {//iterating thru the list of instance variables
            String fieldToString = field.toString();
            String[] fieldToWords = fieldToString.split(" ");
            for (String word : fieldToWords) {
                Matcher matcher = pattern.matcher(word);
                if (matcher.find()) {//a match was found
                    String object = matcher.group();
                    results.add(object);
                    continue;
                }
            }

            System.out.println(fieldToString);
        }
        return results;
    }

    /**
     * Description: Annotates all DTO files given their corresponding abstract syntax trees. This method will also
     * annotate any DTO object files that are included in the given abstract syntax trees. This method will process the
     * DTO files in a depth-first traversal
     *
     * @param tree the abstract syntax trees to use to annotate DTO files
     * @param seen a set of strings representing the DTO files that have already been seen (used to prevent infinite loops)
     */
    public void annotateDTOFiles(AbstractSyntaxTree tree, Set<String> seen, LexicalPreservingPrinter lex) throws FileNotFoundException {//this is a recursive function

        Set<String> toTraverse = new HashSet<>();


        String fileName = tree.getFile().getName();
        fileName = FilenameUtils.removeExtension(fileName);

        seen.add(fileName);//adding current file being annotated to seen set
        CompilationUnit compUnit = tree.getAbstractSyntaxTree();
        List<MethodDeclaration> methods = getMethodDeclarations(compUnit);
        List<FieldDeclaration> instanceVariables = getInstanceVariables(compUnit);

        toTraverse.addAll(annotateGetters(methods));//annotating the DTO file's getter methods
        toTraverse.addAll(getDTOsFromInstanceVariables(instanceVariables));//getting any DTO files present
        createAndWriteNewFiles(tree, compUnit, lex); //creating and writing the new annotated DTO file

        /**
         * Starting recursive traversal of DTO file present in the current DTO file:
         * (Traversing over the files in the toTraverse list)
         */
        if (toTraverse.size() != 0) {//there is at least one DTO file to process, if not then this file is a leaf
            //converting the DTO names into files
            Set<File> DTOFilesToAnnotate = new HashSet<>();
            List<AbstractSyntaxTree> newTrees = new ArrayList<>();
            for (String DTOFileName : toTraverse) {//iterating thru the DTO files to find
                if (!seen.contains(DTOFileName)) {//only adding in new DTO file to annotate if it does not already exist in seen (preventing infinite loops)
                    classNametoFile(DTOFileName, "/Users/kpagkatipunan/Desktop/newPLSCode", DTOFilesToAnnotate);
                    seen.add(DTOFileName);

                }
            }

            for (File file : DTOFilesToAnnotate) {//creating Abstract syntax trees from DTO files
                JavaParser parser = new JavaParser();
                ParseResult<CompilationUnit> temp = parser.parse(file);
                if (temp.isSuccessful()) {
                    Optional<CompilationUnit> opt = temp.getResult();
                    if (opt.isPresent()) {
                        CompilationUnit compilationUnit = opt.get();
                        lex.setup(compilationUnit);
                        newTrees.add(new AbstractSyntaxTree(file, compilationUnit));
                    }
                }
            }//end creating abstract syntax trees from files

            for (AbstractSyntaxTree newTree : newTrees) {//doing recursive calls all new trees
                annotateDTOFiles(newTree, seen, lex);
            }
        }//end recursive case
    }

    /**
     * Description: Writing content and new annotations to new files
     *
     * @param file     the file to create a new (annotated) file from
     * @param compUnit the abstract syntax tree to get the new content to write to the new file
     */
    public void createAndWriteNewFiles(AbstractSyntaxTree file, CompilationUnit compUnit, LexicalPreservingPrinter lex) {
        String fileName = file.getFile().getName();
        String content = compUnit.toString();
        List<Comment> orphanComments = compUnit.getOrphanComments();
        String[] contentList = content.split("\n");
       /* String allContent = writeContent(contentList, orphanComments);*/
        String allContent = lex.print(compUnit);

       /*
        boolean t = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/newFiles").mkdirs();
        File newFile =
                new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/newFiles/" + fileName);*/

        //TODO: THIS IS THE LINE YOUR CHANGED TO ACTUALLY PLACE CHANGES IN PLS FILES
        String path = file.getFile().getAbsolutePath();

        try {
            Files.delete(Paths.get(path));
            File newFile = new File(path); //creating a new file in the same directory as the file to replace

            FileWriter fileWriter = new FileWriter(newFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(allContent);
            bufferedWriter.close();

        } catch (IOException e) {
            System.out.println("Invaild permissions");
        }
    }


    /**
     * Description: Writes new content (swagger annotations, new Todo comments etc.) to the output file. For use by the
     * checkAndWriteAnnotations() method.
     *
     * @param content  the content to write to the new file
     * @param comments list of comments to add to the new file. This parameter is necessary since a simple toString()
     *                 on the abstract syntax tree of the file omits any javaDoc comments that exist in between import
     *                 statements and class definitions.
     * @return A string representing the content of the new file
     */
    public String writeContent(String[] content, List<Comment> comments) {
        //writes content to new file. Have to add any javaDoc comments in between import statements and class definition
        // using this method since a simple toString() omits them.

        Queue<Comment> commentQueue = new LinkedList<>(comments);

        //TODO: WHAT IF YOU JUST USE THE LEXICAL PRESERVATION PRINTER TP PRINT THE AST?
        String toReturn = "";
        int line = 0;
        for (line = 0; line < content.length; line++) {
            String curr = content[line];
            toReturn += curr + "\n";
            if (line + 1 < content.length) {//ensuring index stays in bounds of content array
                String next = content[line + 1];//looking ahead to see if curr is the last import statement
                if (next != null && (!next.startsWith("import")) && !commentQueue.isEmpty()) {
                    //current string is the last import statement so add javaDoc comment
                    Comment comment = commentQueue.peek();
                    if (comment != null) {//a comment to be written exists
                        toReturn += "\n";
                        toReturn += "/**";
                        toReturn += comment.getContent();
                        toReturn += "*/";
                        toReturn += "\n";
                        commentQueue.poll();
                    }
                }
            }
        }//end write
        return toReturn;
    }

    /**
     * Description: Finds all DTO objects in the current abstract syntax tree and creates Abstract Syntax Tree objects
     * from the DTO files present in the given abstract syntax tree. These abstract syntax trees will be used to
     * annotate their corresponding DTO files.
     *
     * @param tree the abstract syntax tree of the file to search for DTO objects
     * @param seen the list of DTO files that have already been seen (used to prevent infinite loops)
     * @return a list of abstract syntax trees representing the DTO files to annotate
     */
    public List<AbstractSyntaxTree> createASTsFromDTOFile(AbstractSyntaxTree tree, Set<String> seen, LexicalPreservingPrinter lex) throws FileNotFoundException {
        List<AbstractSyntaxTree> trees = new ArrayList<>();

        CompilationUnit compUnit = tree.getAbstractSyntaxTree();

        Set<File> DTOsToAnnotate = retrieveDTOs(compUnit);//Retrieving the DTO files to annotate

        JavaParser parser = new JavaParser();
        for (File file : DTOsToAnnotate) {//creating abstract syntax trees from the DTO files to annotate
            String fileName = file.getName();
            fileName = FilenameUtils.removeExtension(fileName);
            seen.add(fileName);
            ParseResult<CompilationUnit> result = parser.parse(file);
            if (result.isSuccessful()) {
                Optional<CompilationUnit> opt = result.getResult();
                if (opt.isPresent()) {
                    CompilationUnit compilationUnit = opt.get();
                    lex.setup(compilationUnit);
                    AbstractSyntaxTree newTree = new AbstractSyntaxTree(file, compilationUnit);
                    trees.add(newTree);
                }
            }
        }//end creating list of ASTs of DTO files to annotate
        return trees;
    }


    /**
     * Description: Searches the given file directory for the target file to search for. If the given directory is not
     * a directory or if the target file cannot be found then null will be returned.
     *
     * @param directory        the directory to search for the target file
     * @param fileNameToSearch the file to search for
     * @return the path to the target file, null if not found
     */
    public List<String> searchDirectory(File directory, String fileNameToSearch) {
        List<String> result = new ArrayList<>();
        if (directory.isDirectory()) {
            search(directory, fileNameToSearch, result);
        } else {
            return null;
        }

        return result;
    }

    /**
     * Description: The recursive helper method for use by the searchDirectory() method. If permission is not granted
     * to search the file directory or if the target file cannot be found, null will be returned.
     *
     * @param file             the directory to search in
     * @param fileNameToSearch the target file to search for
     */
    private void search(File file, String fileNameToSearch, List<String> result) {

        if (file.isDirectory()) {
            //System.out.println("Searching directory ... " + file.getAbsoluteFile());
            //do you have permission to read this directory?
            if (file.canRead()) {
                for (File temp : file.listFiles()) {
                    if (temp.isDirectory()) {
                        search(temp, fileNameToSearch, result);
                    } else {
                        String tempName = temp.getName();
                        //  System.out.println("Current file: " + tempName);
                        //  System.out.println("fileNameToSearch: " + fileNameToSearch);
                        if (fileNameToSearch.equalsIgnoreCase(tempName)) {//the current file is the target file
                            result.add(temp.getAbsoluteFile().toString());
                        }
                    }
                }
            }
        }
    }


    /**
     * Description: Searches for the file corresponding to the parameter name. Adds the file that is found to the results
     * list. If the file is not found, no file is added
     *
     * @param className     the parameter to search for
     * @param directoryPath the directory to search in for the parameter
     * @param results       the map to add the target file to
     */
    public void classNametoFile(String className, String directoryPath, Set<File> results) {
        File plsCoreDirectory = new File(directoryPath);
        String toSearch = className + ".java";
        List<String> DTOFilePaths = searchDirectory(plsCoreDirectory, toSearch);
        //^Finding the DTO file corresponding to the DTO object
        if (DTOFilePaths.size() != 0) {//adding the DTO file to the results list
            String DTOFilePath = DTOFilePaths.get(0);
            File DTOFile = new File(DTOFilePath);
            results.add(DTOFile);
        }
    }

    /**
     * Description: Finds all DTO files in a file's method declarations, import statements, and instance variables.
     * <p>
     * (For use by the createASTsFromDTOFile method)
     *
     * @return a list of all DTO files in a collection of method declarations
     */

    public Set<File> retrieveDTOs(CompilationUnit compUnit) {
        List<MethodDeclaration> methods = getMethodDeclarations(compUnit);
        Set<File> results = new HashSet<>();
        Set<String> DTOsSeen = new HashSet<>();
        for (MethodDeclaration method : methods) {
            //Checking for DTOs in the method body
            Optional<BlockStmt> opt = method.getBody();
            if (opt.isPresent()) {
                BlockStmt methodBody = opt.get();
                String[] methodBodyStringToWords = methodBody.toString().split(" ");
                Set<String> DTOsPresent = createListOfDTOsPresentInMethod(methodBodyStringToWords);
                for (String DTO : DTOsPresent) {//iterating thru DTOs present in the method declaration
                    if (!DTOsSeen.contains(DTO)) {//if the current DTO object has not been seen yet

                        classNametoFile(DTO, "/Users/kpagkatipunan/Desktop/newPLSCode", results);
                        DTOsSeen.add(DTO);

                    }
                }
            }//end retrieving DTOs from method body

            //Checking for DTOs in the method declaration
            String[] methodDeclToWords = method.getDeclarationAsString().split(" ");
            Set<String> DTOsPresent = createListOfDTOsPresentInMethod(methodDeclToWords);
            for (String DTO : DTOsPresent) {//iterating thru DTOs present in the method declaration
                if (!DTOsSeen.contains(DTO)) {
                    classNametoFile(DTO, "/Users/kpagkatipunan/Desktop/newPLSCode", results);
                    DTOsSeen.add(DTO);
                }
            }
        }//end retrieving DTOs from method declaration

        NodeList<ImportDeclaration> imports = compUnit.getImports();
        for (ImportDeclaration importDeclaration : imports) {//finding DTO files in import statements
            String importName = importDeclaration.getNameAsString();
            if (importName.endsWith("DTO")) {
                String[] importNameToWords = importName.split("\\.");
                importName = importNameToWords[importNameToWords.length - 1];//the name of the DTO object is the last element in the array
                if (!DTOsSeen.contains(importName)) {//if the current DTO object has not been seen yet
                    if (!DTOsSeen.contains(importName)) {
                        classNametoFile(importName, "/Users/kpagkatipunan/Desktop/newPLSCode", results);
                        DTOsSeen.add(importName);
                    }
                }
            }
        }

        List<FieldDeclaration> fields = getInstanceVariables(compUnit);
        Set<String> DTOsToAnnotate = getDTOsFromInstanceVariables(fields);//finding DTO files in instance variables
        for (String DTO : DTOsToAnnotate) {
            if (!DTOsSeen.contains(DTO)) {//if the current DTO object has not been seen yet
                classNametoFile(DTO, "/Users/kpagkatipunan/Desktop/newPLSCode", results);
                DTOsSeen.add(DTO);
            }
        }
        return results;
    }


    /**
     * Description: Prints out all files that are RESTServices.
     */
    public void printRESTServices() {
        for (AbstractSyntaxTree tree : this.RESTServices) {
            System.out.println(tree.getFile());
        }
    }

    /**
     * Description: adds files to the list of files
     *
     * @param files files to add
     */
    public void setRESTServices(List<File> files) {
        for (File file : files) {
            this.files.add(file);
        }
    }

    /**
     * Description: Standard getter for retrieving all REST service files.
     *
     * @return list of all REST service files
     */
    public Set<AbstractSyntaxTree> getRESTServices() {
        return this.RESTServices;
    }

    /**
     * Description: Finds all elements in a file directory that are PLS files and adds them to the list of files to search for.
     * This method is for use by the main method (temporary) to extract the REST service files that result from the output
     * of this method
     *
     * @param files
     * @return list of files in the target directory
     */
    public void getFiles(File[] files) {
        List<File> results = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {//the current element is a directory
                getFiles(file.listFiles()); // Calls same method again.
            } else {//the current element is a file
                // if(file.getName().equalsIgnoreCase("LDAPAuthResource.java")){
                results.add(file);//adding file to results
                //}
            }
        }
        setRESTServices(results);
    }

    public static void main(String[] args) throws IOException {

        /*File file1 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/OperationSuccessTestFile.java");
        File file2 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/OperationFailedTestFile.java");
        File file3 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/ParameterSuccessTestFile.java");
        File file4 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/ParameterFailedTestFile.java");
        File file5 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/ApiResponseSuccessTestFile.java");
        File file6 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/ApiResponseFailedTestFile.java");
        File file7 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/DoNotAnnotateTestFile1.java");
        File file8 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/DoNotAnnotateTestFile2.java");
        File file9 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/DoNotAnnotateTestFile3.java");
        File file10 = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/ApiResponseCornerCaseTestFile.java");


        files.add(file1);
        files.add(file2);
        files.add(file3);
        files.add(file4);
        files.add(file5);
        files.add(file6);
        files.add(file7);
        files.add(file8);
        files.add(file9);
        files.add(file10);
*/

        //Deciding to do a depth-first search for REST files
        int dtosAnnotated = 0;
        ArrayList<File> files = new ArrayList<File>();
        LexicalPreservingPrinter lex = new LexicalPreservingPrinter();
     /*   java.nio.file.Path directoryPath = Paths.get("/Users/kpagkatipunan/Desktop/Projects/pls-api");
        //retrieving files from the file directory of interest
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath)){
            for(Path entry: stream){//iterating thru the entries of the stream
                System.out.println(entry.toString());
                String name = entry.getFileName().toString();
                if(Files.isDirectory(entry)){//only annotating the files inside the src folder


                }

            }
        }catch(IllegalStateException e){
            e.printStackTrace();
        }*/

        File PLSDirectory = new File("/Users/kpagkatipunan/Desktop/newPLSCode");
        ArrayList<String> annotations = new ArrayList<>();
        annotations.add("@Operation");
        annotations.add("@Parameter");
        annotations.add("@Schema");
        annotations.add("@ApiResponse");
        FilesToAnnotateFinder finder = new FilesToAnnotateFinder(files);

        List<File> filesToSearch = new ArrayList<>();
        for (File file : PLSDirectory.listFiles()) {//retrieving file directories in PLS Directory that have pls in the name
            //(all config directories etc. are ignored)
            if (file.getName().contains("pls")) {//file name contains pls, adding to filesToSearch
                filesToSearch.add(file);
            }
        }

        File[] filesToSearchArray = filesToSearch.toArray(new File[0]);
        finder.getFiles(filesToSearchArray);
        finder.findRESTServices(lex);
        FileBuilder fileBuilder = new FileBuilder(finder.RESTServices, annotations);
        fileBuilder.checkAndWriteAnnotations(lex);

        for (AbstractSyntaxTree tree : finder.RESTServices) {//iterating thru the REST service files
            Set<String> seen = new HashSet<>();
            List<AbstractSyntaxTree> trees = finder.createASTsFromDTOFile(tree, seen, lex);//creating abstract syntax trees of the DTO files present in the REST file
            for (AbstractSyntaxTree newTree : trees) {//iterating thru the DTO files present in the current REST file
                finder.annotateDTOFiles(newTree, seen, lex);
                dtosAnnotated++;
                //^The above call performs a depth first search for all DTO files
            }
        }


        System.out.println("Annotated " + finder.RESTServices.size() + " resource.java files.");
        System.out.println("Annotated " + dtosAnnotated + " DTO files.");
    }

}

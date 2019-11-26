import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.ws.rs.core.Context;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * Written by Kevin Pagkatipunan (8/1/19)
 * <p>
 * Description: This class creates copies of the given RESTServicesFiles and writes Swagger annotations to the appropriate locations
 * within the file
 */

public class FileBuilder {
    private Set<AbstractSyntaxTree> RESTServicesFiles;//RESTServicesFiles to be annotated
    private List<AbstractSyntaxTree> DTOFiles; //DTO files to be annotated
    private List<String> annotations;//list of annotations to add to RESTServicesFiles

    /**
     * Description: Defualt contructor for FileBuilder
     *
     * @param RESTServicesFiles List of RESTServicesFiles to annotate (represented in the form of an Abstract Syntax Tree)
     * @param annotations       List of annotations to add to RESTServicesFiles
     */
    public FileBuilder(Set<AbstractSyntaxTree> RESTServicesFiles, ArrayList<String> annotations) {
        this.RESTServicesFiles = RESTServicesFiles;
        this.annotations = annotations;
    }

    /**
     * Description: Standard setter for setting the DTO files to annotate
     *
     * @param DTOFiles
     */
    public void setDTOFiles(List<AbstractSyntaxTree> DTOFiles) {
        this.DTOFiles = DTOFiles;
    }

    /**
     * Description: Standard getter for retrieving the DTO files to annotate
     *
     * @return list of DTO files to be annotated
     */
    public List<AbstractSyntaxTree> getDTOFiles() {
        return DTOFiles;
    }

    /**
     * Description: Standard getter for retrieving RESTServicesFiles to annotate
     *
     * @return the RESTServicesFiles to annotate
     */
    public Set<AbstractSyntaxTree> getRESTServicesFiles() {
        return this.RESTServicesFiles;
    }

    /**
     * Description: Standard setter for setting RESTServicesFiles to annotate
     *
     * @param RESTServicesFiles RESTServicesFiles to annotate
     */
    public void setRESTServicesFiles(Set<AbstractSyntaxTree> RESTServicesFiles) {
        this.RESTServicesFiles = RESTServicesFiles;
    }

    /**
     * Description: Standard getter for retrieving annotations to add to RESTServicesFiles
     *
     * @return the annotations to add
     */
    public List<String> getAnnotations() {
        return this.annotations;
    }

    /**
     * Description: Standard setter for setting the annotations to add
     *
     * @param annotations the annotations to add
     */
    public void setAnnotations(ArrayList<String> annotations) {
        this.annotations = annotations;
    }

    /**
     * Description: Creates a hashmap of all methods in the file (as MethodDeclaration nodes) to the JavaDoc comments
     * that they belong to. Ensures that any TODO comments are not associated with methods in the return hash since it
     * does not provide useful information when adding swagger annotations
     * Ex) MethodDeclaration -> JavaDoc comment
     *
     * @param nodes nodes to create hashmap from
     * @return the hashmap of methods to comments
     */
    public Map<MethodDeclaration, Node> createNodeToCommentsMap(List<Node> nodes) {

        Map<MethodDeclaration, Node> results = new HashMap<>();
        for (Node node : nodes) {//iterating through all the nodes in the file, searching for the method declarations
            List<MethodDeclaration> methods = getMethodDeclarations(node);
            for (MethodDeclaration method : methods) {
                //^iterating through all the methods in the file looking for the comments
                Optional<Comment> opt = method.getComment(); // each method has a single comment
                if (opt.isPresent()) {
                    Comment comment = opt.get();//retrieving the comment
                    String content = comment.getContent();
                    if (content.toUpperCase().startsWith("TODO")) {//comment is a TODO
                        results.put(method, null);
                    } else {//comment is not a TODO, so place comment with its corresponding method
                        results.put(method, comment);
                    }
                } else {//this method has no comment associated with it
                    results.put(method, null);
                }
            }

        }
        return results;
    }

    /**
     * Description: Converts the list of AnnotationExpr to a list of annotation names of type String. For use by the
     * checkAnnotations() method
     *
     * @param annotations annotations to convert to annotation names
     * @return list of annotation names
     */
    public List<String> toAnnotationNameList(NodeList<AnnotationExpr> annotations) {
        //converts the list of AnnotationExpr to a list of annotation names of type String
        // for use by the checkAnnotations() method

        List<String> results = new ArrayList<>();

        for (AnnotationExpr annotation : annotations) {
            String ann = annotation.toString();
            ann = ann.split("\\(")[0];//retrieving the annotation name (everything before the "()")
            results.add(ann);
        }

        //need to trim the annotationNames
        return results;
    }

    /**
     * Description: Retrieves all @Parameter annotations from target method
     *
     * @param method the method to search for @Parameter annotations
     * @return list of @Parameter annotations
     */
    public List<AnnotationExpr> getParameterAnnotations(MethodDeclaration method) {
        List<AnnotationExpr> results = new ArrayList<>();
        NodeList parameters = method.getParameters();
        for (Object parameter : parameters) {
            com.github.javaparser.ast.body.Parameter p = (com.github.javaparser.ast.body.Parameter) parameter;
            results.addAll(p.getAnnotations());
        }

        return results;
    }

    /**
     * Description: Checks which annotations need to be added to the target file and adds them. Automatically adds the
     * required import statement if it is not already present. Determines whether a TODO comment needs to be added to
     * the file to notify that a annotation must be added by hand.
     */
    public void checkAndWriteAnnotations(LexicalPreservingPrinter lex) {
        for (AbstractSyntaxTree file : this.RESTServicesFiles) {//iterating thru all RESTServicesFiles to annotate
            CompilationUnit compUnit = file.getAbstractSyntaxTree();
            List<Node> nodes = compUnit.getChildNodes();
            List<Node> annotationNodes = new ArrayList<>();
            Map<MethodDeclaration, Node> nodesToWrite = null;

            for (Node node : nodes) {
                //^finding all nodes that support annotations, then creating a map of methods to the comments associated
                // with them. nodesToWrite contains all relevant methods and comments as key-value pairs.
                if (NodeWithAnnotations.class.isAssignableFrom(node.getClass())) {
                    annotationNodes.add(node);
                    nodesToWrite = createNodeToCommentsMap(nodes);
                    System.out.println(nodesToWrite.toString());
                }
            }

            for (Entry<MethodDeclaration, Node> entry : nodesToWrite.entrySet()) {
                //^iterating through map of methods and comments to add swagger annotations to
                MethodDeclaration method = entry.getKey();
                Node comments = entry.getValue();
                NodeList<AnnotationExpr> annotations = new NodeList<>(method.getAnnotations());
                List<AnnotationExpr> parameterAnnotations = getParameterAnnotations(method);
                annotations.addAll(parameterAnnotations);
                List<String> annotationNames = toAnnotationNameList(annotations);
                //^List of annotations that currently exist in the current method

                if (!annotationNames.containsAll(this.annotations) && annotationNames.contains("@Path")) {//adding swagger annotations if they do not already exist
                    // and current method has a @Path annotation
                    putOperationAnnotation(method, comments, annotationNames);
                    putParameterAnnotation(method, comments, annotationNames, compUnit.getImports(), compUnit);
                    putApiResponseAnnotation(method, annotationNames, compUnit);
                }
            }//end adding swagger annotations to file
            createAndWriteNewFiles(file, compUnit, lex); //writing changes to new files
        }//end iterating thru RESTServicesFiles
    }

    /**
     * Description: Creates, writes, and saves a newly annotated file. The new file is saved in the same location as the
     * original file that is replacing.
     *
     * @param file     the file to replace with the newly annotated file
     * @param compUnit the abstract syntax tree including the newly added annotations to write to the new file
     */
    public void createAndWriteNewFiles(AbstractSyntaxTree file, CompilationUnit compUnit, LexicalPreservingPrinter lex) {


        String allContent = lex.print(compUnit);
      /*  String content = compUnit.toString();
        List<Comment> orphanComments = compUnit.getOrphanComments();
        String[] contentList = content.split("\n");
        String allContent = writeContent(contentList, orphanComments);*/

      /*  boolean t = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/newFiles").mkdirs();
        File newFile = new File("/Users/kpagkatipunan/Desktop/swagger-automation/src/main/java/newFiles/" + fileName);
*/
        //^The above two commented lines are for testing purposes

        //TODO: THIS IS THE LINE YOUR CHANGED TO ACTUALLY PLACE CHANGES IN PLS FILES
        String path = file.getFile().getPath();

        try {

            Files.delete(Paths.get(path));
            if (Files.exists(Paths.get(path))) {
                System.out.println("Failure file still exists");
            }
            File newFile = new File(path); //creating a new file in the same directory as the file to replace
            FileWriter fileWriter = new FileWriter(newFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(allContent);
            bufferedWriter.close();
        } catch (IOException e) {
            System.out.println("New file was not created successfully.");
            e.printStackTrace();
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
     * Description: Retrieves the description associated with the JavaDoc comment. For use by the putOperationAnnotation
     * method
     *
     * @param content The string to retrieve the description from
     * @return the description to add into the @Operation annotation
     */
    public String getDescription(String content) {
        String[] words = content.split("@param");//removing any @ characters
        String temp = words[0]; //retrieving resulting string
        words = temp.split("\\n"); //spliting the description at any newline characters
        String result = "";

        for (String element : words) {//building the description w/o any newline characters
            result += element;
        }
        words = result.split("\\*");//splitting the description at any * characters
        result = "";
        for (String element : words) {//building the resulting string without any * characters
            result += element;
        }

        words = result.split(" ");//removing any extra spaces in the middle of the description
        result = "";

        for (String element : words) {//building the resulting description
            if (!element.equals("")) {
                result += element + " ";
            }
        }
        result = result.replace("\"", "\\\"");
        result = result.replace("\r", "");
        return result.trim();
    }

    /**
     * Description: Adds a summary label to swagger @Operation annotations. The summary description generated is based off the
     * method name.
     *
     * @param method the method to annotate
     * @param ann    the annotation to add the summary label to
     */
    public void addSummaryDescription(MethodDeclaration method, NormalAnnotationExpr ann) {
        String name = method.getName().asString();
        ann.addPair("summary", "\"" + name + "\"");
    }

    /**
     * Description: Places a swagger @Operation annotation to the target method and automatically adds the required
     * import statement. Only does this if the @Operation annotation does not already exist, otherwise does nothing.
     *
     * @param node     the method to add the @Operation annotation to
     * @param comments the comment to add to the @Operation annotation
     * @return the method with the @Operation annotation added
     */
    public void putOperationAnnotation(NodeWithAnnotations node, Node comments, List<String> existingAnnotations) {
        if (!existingAnnotations.contains("@Operation")) { //method does not contain an @Operation annotation, so add it
            if (comments != null) {//if there is a javaDoc Comment that exists for this method
                Comment comment = (Comment) comments;
                String description = getDescription(comment.getContent());
                description = "\"" + description + "\"";
                NormalAnnotationExpr ann = node.addAndGetAnnotation(Operation.class);
                ann.addPair("description", description);
                addSummaryDescription((MethodDeclaration) node, ann);
            } else {//no java doc comment associated with method
                //javaDoc Comment does not exist, so add Todo comment
                MethodDeclaration method = (MethodDeclaration) node;
                putTodoComment(method, "@Operation");
            }
        }
    }

    /**
     * Description: Gets all parameter types and returns them in a list. For use by the putParameterAnnotation method
     *
     * @param method The method to find the parameter types in
     * @return a list of all the parameter types expressed as a String
     */
    public List<String> getParameterTypes(MethodDeclaration method) {
        List<String> results = new ArrayList<>();
        NodeList<com.github.javaparser.ast.body.Parameter> parameters = method.getParameters();
        for (com.github.javaparser.ast.body.Parameter parameter : parameters) {
            //iterating thru all parameters in the method declaration
            results.add(parameter.getType().asString());
        }
        return results;
    }

    /**
     * Description: Retrieves all parameters declared by the target method declaration.
     *
     * @param method the target method declaration
     * @return List of parameters defined by the target method declaration
     */
    public List<com.github.javaparser.ast.body.Parameter> getParametersFromMethodDeclaration(MethodDeclaration method) {

        List<com.github.javaparser.ast.body.Parameter> parameters = new ArrayList<>();

        for (Node childNode : method.getChildNodes()) {
            //populating parameters list with all the parameters in the method declaration
            if (com.github.javaparser.ast.body.Parameter.class.isAssignableFrom(childNode.getClass())) {
                //childNode is a method parameter so add it to the parameters list
                parameters.add((com.github.javaparser.ast.body.Parameter) childNode);
            }
        }//end populating list with all parameters

        return parameters;
    }

    /**
     * Description: Places a TODO comment describing which swagger annotations to add manually
     *
     * @param method        the method to add the comment to
     * @param toAddManually the annotation that needs to be added manually
     */
    public void putTodoComment(MethodDeclaration method, String toAddManually) {
        if (toAddManually.equals("@Operation")) {
            String summary = method.getNameAsString();
            NormalAnnotationExpr ann = method.addAndGetAnnotation(Operation.class);
            ann.addPair("description", "\"TODO: Add Description here.\"");
            ann.addPair("summary", "\"" + summary + "\"");
        } else if (toAddManually.equals("@Parameter")) {
            List<com.github.javaparser.ast.body.Parameter> parameters = getParametersFromMethodDeclaration(method);
            for (com.github.javaparser.ast.body.Parameter parameter : parameters) {//iterating thru the list of parameters
                Optional<AnnotationExpr> opt = parameter.getAnnotationByClass(Context.class);
                if (!opt.isPresent()) {//no @Context annotation exists
                    NormalAnnotationExpr ann = parameter.addAndGetAnnotation(Parameter.class);
                    ann.addPair("description", "\"TODO: Add description and @Schema annotation here.\"");
                }
            }
        } else if (toAddManually.equals("@ApiResponse")) {
            NormalAnnotationExpr ann = method.addAndGetAnnotation(ApiResponse.class);
            ann.addPair("description", "\"TODO: Add status codes and descriptions here.\"");
        } else if (toAddManually.equals("Status codes to @ApiResponse")) {
            NormalAnnotationExpr ann = method.addAndGetAnnotation(ApiResponse.class);
            ann.addPair("description", "\"TODO: Add status codes and descriptions here.\"");
        }
    }

    /**
     * Description: Places a swagger @Parameter annotation to the target method and automatically adds the required
     * import statement. Only does this if the @Parameter annotation does not already exist, otherwise does nothing.
     *
     * @param node     the method to add the @Parameter annotation to
     * @param comments the comment to add to the @Operation annotation
     * @param imports  the import statements that currently exist in the file (for use by the putSchemaAnnotation method)
     * @param compUnit the compilation unit or abstract syntax tree representing the current file being annotated
     * @return the method with the @Parameter annotation added
     */
    public void putParameterAnnotation(NodeWithAnnotations node, Node comments, List<String> existingAnnotations, NodeList<ImportDeclaration> imports, CompilationUnit compUnit) {
        //find all parameters in the javaDoc comment, then check if the number of parameter annotations
        //added matches the number of parameters in the method declaration, if not add them with an @Parameter
        MethodDeclaration method = (MethodDeclaration) node;
        List<com.github.javaparser.ast.body.Parameter> parameters = getParametersFromMethodDeclaration(method);

        if (!existingAnnotations.contains("@Parameter")) { //method does not contain an @Parameter annotation, so add it
            List<String> paramDescriptions = new ArrayList<>();//List of the parameter descriptions

            JavadocComment javadocComment = (JavadocComment) comments;
            Javadoc content = null;
            if (javadocComment != null) {//a JavaDoc comment does exist
                content = javadocComment.parse();//use this to add info to parameter annotation
                List<JavadocBlockTag> tags = content.getBlockTags();
                for (JavadocBlockTag tag : tags) {//getting all the parameter descriptions
                    if (tag.getType().toString().equalsIgnoreCase("param")) {//getting all param descriptions
                        paramDescriptions.add(tag.getContent().toText());
                    }
                }

                MethodDeclaration nodeToWrite = (MethodDeclaration) node;
                List<String> paramTypes = getParameterTypes(nodeToWrite);

                Map<com.github.javaparser.ast.body.Parameter, String> parameterDescriptionsAndTypes = new HashMap<>();
                for (int i = 0; i < paramTypes.size(); i++) {//populating map of parameter descriptions to their types

                    String description = "";

                    if (i <= paramDescriptions.size() - 1) {//ensuring that i is in bounds of the paramDescriptions array
                        description = paramDescriptions.get(i);
                    } else {
                        description = "TODO: Add parameter description";
                    }

                    parameterDescriptionsAndTypes.put(parameters.get(i), description);
                }

                Collections.reverse(parameters);//reversing the order of the list to ensure that the correct @Parameter
                //annotation gets added to its corresponding parameter
                for (int i = 0; i < parameters.size(); i++) {//adding parameter annotations
                    com.github.javaparser.ast.body.Parameter parameter = parameters.get(i);
                    Optional<AnnotationExpr> opt = parameter.getAnnotationByClass(Context.class);
                    if (!opt.isPresent()) {//no @Context annotation exists
                        String description = parameterDescriptionsAndTypes.get(parameter);

                        description = description.replace("\n", "");
                        description = description.replace("\r", "");
                        description = description.replace("  ", " ");
                        description = description.replace("\"", "\\\"");
                        description = "\"" + description + "\"";
                        String type = parameter.getTypeAsString();
                        NormalAnnotationExpr ann = parameter.addAndGetAnnotation(Parameter.class);
                        ann.addPair("description", description);
                        String schemaAnnotation = putSchemaAnnotation(type);
                        ann.addPair("schema", schemaAnnotation);
                        if (!imports.contains(Schema.class)) {
                            //the file does not contain the import statement for @Schema annotations, so add it
                            compUnit.addImport(Schema.class);
                        }
                    }//end @Context check
                }//end for loop

            } else {//javaDoc Comment does not exist so add Todo Comment

                //TODO: Add functionality if no JavaDoc comment, then find parameters in method declaration and add
                //Todo comment to add parameter description
                putTodoComment(method, "@Parameter");
            }
        }//end check for existing @Parameter annotation
    }

    /**
     * Description: Creates a string representing the @Schema annotation to be added into the @Parameter annotation.
     * This is for use by the putParameterAnnotation() method.
     *
     * @param type The type to add to the @Schema annotation
     * @return a string representing the @schema annotation with data to add to the @Parameter annotation
     */
    public String putSchemaAnnotation(String type) {

        String results = "@Schema(type = " + "\"" + type + "\"" + ")";
        return results;
    }

    /**
     * Description: Places a swagger @ApiResponse annotation to the target method and automatically adds the required
     * import statement. Performs this action if the @ApiResponse annotation does not already exist, otherwise
     * a check is made to determine if the target method contains BOTH swagger @ApiResponse and enunciate @StatusCodes.
     * If so, then only the @StatusCodes annotations is removed from the file. If not, then nothing happens.
     *
     * @param node                the target method to annotate
     * @param existingAnnotations list of existing annotations in the target method
     */
    public void putApiResponseAnnotation(Node node, List<String> existingAnnotations, CompilationUnit compUnit) {
        NodeWithAnnotations annotatedNode = (NodeWithAnnotations) node;
        NodeList<AnnotationExpr> annotations = annotatedNode.getAnnotations();

        if (!existingAnnotations.contains("@ApiResponse")) {
            //method does not contain an @ApiOperation annotation, so add it
            List<StatusCodeDescriptionPair> statusCodes = new ArrayList<>();

            for (AnnotationExpr annotation : annotations) {//finding all status codes in each method
                if (SingleMemberAnnotationExpr.class.isAssignableFrom(annotation.getClass()) && (annotation.getName().asString().equals("StatusCodes") || annotation.getName().asString().equals("Warnings"))) {
                    //getting enunciate @StatusCodes and @Warnings annotations
                    statusCodes.addAll(getStatusCodes(annotation));
                }
            }
            if (statusCodes.size() == 0) {//if there are no status codes available
                //javaDoc Comment does not exist, so add Todo comment
                MethodDeclaration method = (MethodDeclaration) node;
                putTodoComment(method, "@ApiResponse");
            } else {//there are status codes to write
                for (StatusCodeDescriptionPair codeDescriptionPair : statusCodes) {
                    //^placing each status code in a @ApiResponse
                    String code = codeDescriptionPair.getCode();
                    String description = codeDescriptionPair.getDescription();
                    code = "\"" + code + "\"";
                    description = "\"" + description + "\"";
                    NodeWithAnnotations nodeToWrite = (NodeWithAnnotations) node;
                    NormalAnnotationExpr ann = nodeToWrite.addAndGetAnnotation(ApiResponse.class);
                    ann.addPair("responseCode", code);
                    ann.addPair("description", description);
                }

                //Removing the @StatusCode and @ResponseCode annotations and both their import statements
                removeStatusCodeAnnotations(annotations, compUnit);
            }
        } else if (existingAnnotations.contains("@ApiResponse") && existingAnnotations.contains("@StatusCodes")) {
            //^method contains BOTH @ApiResponse and @StatusCodes annotations, so remove the @StatusCodes only
            removeStatusCodeAnnotations(annotations, compUnit);
        }
    }

    /**
     * Description: Removes the enunciate @StatusCodes annotation along with the @ResponseCode annotation nested inside
     * of it. Also gets rid of the corresponding import statements in the target file.
     *
     * @param annotations The list of annotations that currently exist in the method
     * @param compUnit    The file to remove the annotations from (represented as an abstract syntax tree)
     */
    public void removeStatusCodeAnnotations(List<AnnotationExpr> annotations, CompilationUnit compUnit) {
        AnnotationExpr annotationToRemove = null;
        for (AnnotationExpr annotation : annotations) {
            if (SingleMemberAnnotationExpr.class.isAssignableFrom(annotation.getClass()) && annotation.getName().asString().startsWith("StatusCodes")) {//annotation is a @StatusCode annotation
                annotationToRemove = annotation;
                break;
            }
        }//end remove @StatusCode annotation

        annotations.remove(annotationToRemove);

        NodeList<ImportDeclaration> imports = compUnit.getImports();

        ImportDeclaration statusCodeImport = null;
        ImportDeclaration responseCodeImport = null;
        for (ImportDeclaration importDeclaration : imports) {
            //iterating thru import statements to remove the status codes import statement
            if (importDeclaration.getName().asString().equals(("com.webcohesion.enunciate.metadata.rs.StatusCodes"))) {
                //removing StatusCodes import statement
                statusCodeImport = importDeclaration;

            } else if (importDeclaration.getName().asString().equals(("com.webcohesion.enunciate.metadata.rs.ResponseCode"))) {
                //removing ResponseCode import statement
                responseCodeImport = importDeclaration;
            }

            if (statusCodeImport != null && responseCodeImport != null) {//both imports to remove were found
                break;
            }
        }//end removing status code import statement
        imports.remove(statusCodeImport);
        imports.remove(responseCodeImport);
    }

    /**
     * Description: Gets the status codes to add to the @ApiResponse annotation
     *
     * @param method the method to retrieve the status codes from
     * @return a HashMap of the status code to the condition that causes it
     */
    public List<StatusCodeDescriptionPair> getStatusCodes(AnnotationExpr method) {
        //used to get the status codes documented by enunciate annotations in method declarations
        //for use by the putApiResponseAnnotation() method

        List<StatusCodeDescriptionPair> results = new ArrayList<>(); //Change to something else, this cannot handle duplicate response codes

        List<Node> exprChildren = method.getChildNodes();

        for (Node child : exprChildren) {//iterating thru children to find status codes
            if (ArrayInitializerExpr.class.isAssignableFrom(child.getClass())) {
                ArrayInitializerExpr exp = (ArrayInitializerExpr) child;
                getStatusCodesAndDescriptions(exp, results);
            }
        }
        return results;
    }

    /**
     * Description: Populates a hashmap of status codes to their corresponding description.
     *
     * @param codes   The expected status codes
     * @param results The hashmap to populate with status codes and descriptions
     */
    public void getStatusCodesAndDescriptions(ArrayInitializerExpr codes, List<StatusCodeDescriptionPair> results) {

        List<Node> children = codes.getChildNodes();
        for (Node child : children) {
            NormalAnnotationExpr codeDescriptionPair = (NormalAnnotationExpr) child;
            NodeList list = codeDescriptionPair.getPairs();
            Entry<Integer, String> pair = getCodeDescriptionPair(list.get(0).toString(), list.get(1).toString());
            if (pair == null) {//add TODO comment
                MethodDeclaration method = (MethodDeclaration) child;
                putTodoComment(method, "Status codes to @ApiResponse");
            } else {//add code description pair to the results
                StatusCodeDescriptionPair statusCodeDescriptionPair = new StatusCodeDescriptionPair(String.valueOf(pair.getKey()), pair.getValue());
                results.add(statusCodeDescriptionPair);
            }
        }
    }

    /**
     * Description: Retrieving the status codes and descriptions
     *
     * @param code        The status code to retrieve
     * @param description The description to retrieve
     * @return A key-value pair representing the status code and description
     */
    public Entry<Integer, String> getCodeDescriptionPair(String code, String description) {

        String actualDescription = description.replaceAll("condition = ", "");
        String finalCode = code.replaceAll("[^0-9]", "");
        actualDescription = actualDescription.replaceAll("\"", "");//removing any extra double quotes
        actualDescription = actualDescription.replaceAll("\\+", "");//removing any "+" signs in the text
        actualDescription.trim();
        if (finalCode.length() == 0 || actualDescription.length() == 0) {
            return null;
        } else {

            final class Pair<String, V> implements Entry<String, String> {

                private final String key;
                private String value;

                public Pair(String key, String value) {
                    this.key = key;
                    this.value = value;
                }

                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public String getValue() {
                    return value;
                }

                @Override
                public String setValue(String value) {
                    String old = this.value;
                    this.value = value;
                    return old;
                }
            }

            Pair codeDescriptionPair = new Pair<String, String>(finalCode, actualDescription);
            return codeDescriptionPair;
        }
    }

    /**
     * Description: Iterates through the child nodes of the target node, finding all the method declarations inside
     *
     * @param node the node to search for method declarations
     * @return a list of method declarations
     */
    public List<MethodDeclaration> getMethodDeclarations(Node node) {
        List<MethodDeclaration> methods = new ArrayList<>();
        List<Node> children = node.getChildNodes();
        for (Node currNode : children) {
            if (MethodDeclaration.class.isAssignableFrom(currNode.getClass())) {//currNode is a MethodDeclaration
                methods.add((MethodDeclaration) currNode);
            }
        }
        return methods;
    }
}

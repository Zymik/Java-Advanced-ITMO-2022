package info.kgeorgiy.ja.kosolapov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Generates implementation of classes.
 *
 * @author Ivan Kosolapov
 */

public class Implementor implements Impler, JarImpler {

    /**
     * {@code System.lineSeparator()}
     */
    private static final String SEPARATOR = System.lineSeparator();
    /**
     * Default tabulation for lines in generated class
     */
    private static final int TABULATION_SIZE = 4;
    /**
     * {@code SEPARATOR.repeat(2)}
     */
    private static final String TWO_SEPARATORS = SEPARATOR.repeat(2);


    @Override
    public void implement(Class<?> aClass, Path path) throws ImplerException {
        implementClass(aClass, path);
    }


    @Override
    public void implementJar(Class<?> aClass, Path path) throws ImplerException {
        implementJarClass(aClass, path);
    }

    /**
     * Check format of arguments that passed to main.<p>
     * There are two types of it <ol>
     * <li><var>*.jar</var> and two non-null strings</li>
     * <li>two non-null string</li>
     * </ol>
     *
     * @param args arguments to check.
     * @return true if arguments valid.
     */
    private static boolean checkArgs(String[] args) {
        return args.length == 2 && args[0] != null && args[1] != null ||
                args.length == 3 && "-jar".equals(args[0]) && args[1] != null && args[2] != null;
    }


    /**
     * Converts {@code s} to Unicode
     *
     * @param s string to encode
     * @return encoded string
     */
    //I copied it
    private static String unicodeRepresentation(String s) {
        return s.chars().mapToObj(c -> c < 128 ? String.valueOf((char) c) : String.format("\\u%04X", c)).collect(Collectors.joining());
    }

    /**
     * Run {@link #implementJarClass(Class, Path)} if {@code "-jar".equals(args[0])} and
     * {@link #implement(Class, Path)} on arguments.
     *
     * @param args command line arguments.
     */

    public static void main(String[] args) {

        if (!checkArgs(args)) {
            System.err.println("Invalid input format");
            return;
        }
        int i = "-jar".equals(args[0]) ? 1 : 0;
        Class<?> aClass;
        try {
            aClass = Class.forName(args[i]);
        } catch (ClassNotFoundException e) {
            System.err.println("Invalid classname: " + args[i]);
            return;
        }
        Path path = Path.of(args[i + 1]);
        try {
            if (i == 0) {
                implementClass(aClass, path);
            } else {
                implementJarClass(aClass, path);
            }
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        }

    }

    /**
     * Compiles {@code aClass} implementation resolving from {@code path}
     *
     * @param aClass which implementation should be compiled.
     * @param path   place where should class be compiled.
     * @throws ImplerException if can not compile or can not resolve classpath.
     */
    private static void compileClass(Class<?> aClass, Path path) throws ImplerException {
        var filepath = implementClass(aClass, path);
        var compiler = ToolProvider.getSystemJavaCompiler();
        Path classpath;
        try {
            classpath = Path.of(aClass.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | NullPointerException e) {
            classpath = Path.of("");
        }
        String[] args = new String[]{filepath.toString(), "-cp", path.getParent().resolve(classpath).toString()};
        int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Can not compile: " + filepath);
        }
    }


    /**
     * Produces <var>.jar</var> file implementing class or interface specified by provided <var>token</var>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <var>Impl</var> suffix
     * added.
     *
     * @param aClass type token to create implementation for.
     * @param path   target <var>.jar</var> file.
     * @throws ImplerException when implementation cannot be generated.
     */

    public static void implementJarClass(Class<?> aClass, Path path) throws ImplerException {
        Path tmp;
        try {
            Path parent = path.getParent();
            if (parent == null) {
                parent = Path.of("/");
            }
            tmp = Files.createTempDirectory(parent, "tmp");
        } catch (IOException e) {
            throw new ImplerException("Can not create temporary directory for files: " + e.getMessage(), e);
        }
        try {
            compileClass(aClass, tmp);
            produceJar(aClass, path, tmp);
        } finally {
            try (var stream = Files.walk(tmp)) {
                for (var p : stream.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            } catch (IOException e) {
                throw new ImplerException("Exception while deleting temporary dir: " + e.getMessage(), e);
            }
        }

    }

    /**
     * Produce <var>*.jar</var> from class at path {@code tmp.resolve(String.format("%s/%s%s", aClass.getPackageName().replace('.', '/'),
     * aClass.getSimpleName(), "Impl.class"))}.
     *
     * @param aClass type token which implementation was generated
     * @param path   path to <var>*.jar</var>.
     * @param tmp    temporary directory for implementation class.
     * @throws ImplerException if <var>*.class</var> is not exist or other problems while writing to <var>*.jar</var>.
     */
    private static void produceJar(Class<?> aClass, Path path, Path tmp) throws ImplerException {
        var manifest = new Manifest();
        var attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (var jarOutputStream = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            var classname = String.format("%s/%s%s", aClass.getPackageName().replace('.', '/'),
                    aClass.getSimpleName(), "Impl.class");
            jarOutputStream.putNextEntry(new ZipEntry(classname));
            Files.copy(tmp.resolve(classname), jarOutputStream);
        } catch (IOException e) {
            throw new ImplerException("Exception while writing to jar: " + e.getMessage(), e);
        }

    }


    /**
     * Generates path to {@code aClass} implementation like it describes in {@link #implementClass(Class, Path)}
     *
     * @param aClass type token ot generate path for
     * @param path   root directory
     * @return {@link Path} to {@code aClass} implementation
     */
    private static Path getClassPath(Class<?> aClass, Path path) {
        String packageName = aClass.getPackageName();
        Path packagePath = Path.of(path.toString(),
                aClass.getPackageName().split("\\."));
        String className = aClass.getSimpleName() + "Impl";
        return packagePath.resolve(className + ".java");
    }


    /**
     * Produces code implementing class or interface specified by provided {@code aClass}.
     * <p>
     * Generated class classes name should be same as classes name of the type token with {@code Impl} suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * {@code path} directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to {@code $root/java/util/ListImpl.java}
     *
     * @param aClass type token to create implementation for.
     * @param path   root directory.
     * @return path to implemented class.
     * @throws ImplerException when implementation cannot be
     *                         generated.
     */

    public static Path implementClass(Class<?> aClass, Path path) throws ImplerException {

        int modifiers = aClass.getModifiers();
        if (availableClass(aClass, modifiers)) {
            throw new ImplerException("Must public, protected, package-private class or interface " +
                    "not final or sealed class: " + aClass.getCanonicalName());
        }
        String packageName = aClass.getPackageName();

        Path packagePath = Path.of(path.toString(),
                aClass.getPackageName().split("\\."));
        try {
            Files.createDirectories(packagePath);
        } catch (IOException ignored) {
        }

        String className = aClass.getSimpleName() + "Impl";
        Path classPath = packagePath.resolve(className + ".java");
        try (var writer = Files.newBufferedWriter(classPath)) {
            writer.write(unicodeRepresentation(generateClass(aClass, packageName)));
        } catch (IOException e) {
            throw new ImplerException("Exception while writing class to Output: " + e.getMessage(), e);
        }
        return classPath;
    }

    private static boolean availableClass(Class<?> aClass, int modifiers) {
        return aClass.isPrimitive() || aClass.isEnum() || aClass.isSealed()
                || aClass == Enum.class || aClass.isArray()
                || Modifier.isFinal(modifiers) || aClass.isAnonymousClass()
                || aClass.isRecord() || Modifier.isPrivate(modifiers);
    }


    /**
     * Produces {@link String} by concatenation of value of {@code toStringFromIndex} on range from 0 to size
     * with {@code Separator}
     *
     * @param size              right border of range, exclude
     * @param toStringFromIndex function from index to {@link String}
     * @param separator         {@link String} that will separate values from {@code toStringFromIndex}
     * @return concatenated separated values if {@code toStringFromIndex}
     */
    private static String toStringFromIndexWithSeparator(int size, IntFunction<String> toStringFromIndex,
                                                         String separator) {
        return IntStream.range(0, size)
                .mapToObj(toStringFromIndex)
                .collect(Collectors.joining(separator));
    }

    /**
     * Makes {@link String} of {@code depth * }{@value #TABULATION_SIZE} spaces
     *
     * @param depth describes tabulation depth
     * @return {@link String} of  exact count of spaces
     */
    private static String tabulation(int depth) {
        return " ".repeat(depth * TABULATION_SIZE);
    }

    /**
     * Start with {@code depth} tabulation depth
     *
     * @param depth tabulation depth
     * @return {@code SEPARATOR + tabulation(depth)}
     * @see #tabulation(int)
     */
    private static String startNewLine(int depth) {
        return SEPARATOR + tabulation(depth);
    }


    /**
     * Get default value of {@code t}
     *
     * @param t type token will default value will be taken
     * @return if t - {@code void.class} "". if t - {@code boolean.class} "false". In other primitives return "0".
     * Else return "null"
     */

    private static String defaultValueString(Class<?> t) {
        if (t == void.class) {
            return "";
        } else if (t == boolean.class) {
            return "false";
        } else if (t.isPrimitive()) {
            return "0";
        } else {
            return "null";
        }
    }

    /**
     * Produce {@code int} that describes {@code modifiers} without modifier
     *
     * @param modifiers {@code int} describe modifiers
     * @param modifier  {@code int} describe modifier that must be removed
     * @return {@code (modifiers | modifier) ^ modifier}
     */
    private static int removeModifier(int modifiers, int modifier) {
        return (modifiers | modifier) ^ modifier;
    }

    /**
     * Produces class name of {@code aClass} that will be able to compile.
     *
     * @param aClass type token which name will be produced
     * @return name of class. If class is nested replace all $ to dots in {@code aClass.getTypeName()}, else
     * return {@code aClass.getTypeName()}.
     */
    private static String getClassName(Class<?> aClass) {
        List<Class<?>> buffer = new ArrayList<>();
        for (var i = aClass; i != null; i = i.getEnclosingClass()) {
            buffer.add(i);
        }
        var builder = new StringBuilder();
        builder.append(buffer.get(buffer.size() - 1).getTypeName());
        for (int i = buffer.size() - 2; i >= 0; i--) {
            builder.append(".")
                    .append(buffer.get(i).getSimpleName());
        }
        return builder.toString();
    }

    /**
     * Generates code implementing class or interface specified by provided {@code aClass}.
     *
     * @param aClass type token to create implementation for.
     * @param pack   package name
     * @return Generated source code
     * @throws ImplerException when implementation cannot be generated.
     */
    private static String generateClass(Class<?> aClass, String pack) throws ImplerException {
        StringBuilder builder = new StringBuilder();
        if (!pack.isEmpty()) {
            builder.append("package ")
                    .append(pack)
                    .append(";");
        }
        builder.append(TWO_SEPARATORS)
                .append("public class ")
                .append(aClass.getSimpleName())
                .append("Impl")
                .append(aClass.isInterface() ? " implements " : " extends ")
                .append(getClassName(aClass))
                .append(" {")
                .append(TWO_SEPARATORS)
                .append(generateAllConstructors(aClass))
                .append(TWO_SEPARATORS)
                .append(generateAllMethods(aClass))
                .append(TWO_SEPARATORS)
                .append("}");
        return builder.toString();
    }

    /**
     * Generates {@link String} of {@code executable}.
     *
     * @param returnTypeAndName   {@link String} that will be in place of return type and name of executable
     * @param body                {@link String} that will be inside of method or constructor
     * @param parametersInBracket parameters that describes method/constructor signature
     * @param executable          {@link Executable} which
     * @return {@link String} of generated constructor
     */
    private static String generateConstructorOrMethod(String returnTypeAndName,
                                                      String parametersInBracket,
                                                      String body,
                                                      Executable executable) {
        int modifiers = executable.getModifiers();
        modifiers = removeModifier(modifiers, Modifier.ABSTRACT);
        modifiers = removeModifier(modifiers, Modifier.TRANSIENT);
        StringBuilder builder = new StringBuilder();
        builder.append(tabulation(1))
                .append(Modifier.toString(modifiers))
                .append(" ")
                .append(returnTypeAndName)
                .append(parametersInBracket);
        Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            String exceptionSignature = toStringFromIndexWithSeparator(exceptions.length,
                    i -> getClassName(exceptions[i]), ",");
            builder.append(" throws ")
                    .append(exceptionSignature);
        }
        builder.append(" {")
                .append(startNewLine(1 + 1))
                .append(body).append(";")
                .append(startNewLine(1))
                .append("}");
        return builder.toString();
    }




    /**
     * Generate {@link String} that represents constructor of
     * {@code constructor.getDeclaringClass().getSimpleName() + "Impl"} class that just passes arguments
     * to {@code constructor} as a super-constructor
     *
     * @param constructor which would be used as super. New constructor will have same parameters as {@code constructor}
     * @return {@link String} of generated constructor
     */
    private static String generateConstructor(Constructor<?> constructor) {
        String parametersInBracket = parametersInBracket(constructor);
        return generateConstructorOrMethod(constructor.getDeclaringClass().getSimpleName() + "Impl",
                parametersInBracket, "super" + parametersInBracketWithoutType(constructor), constructor);
    }

    /**
     * Generate {@link String}  of default implementation of method, which ignores all arguments
     * and return default value
     *
     * @param method method to generate default implementation of
     * @return {@link String} of generated method
     */
    private static String generateMethod(Method method) {
        int m = method.getModifiers();
        return generateConstructorOrMethod(getClassName(method.getReturnType()) + " " + method.getName(),
                parametersInBracket(method), "return " + defaultValueString(method.getReturnType()), method);
    }


    /**
     * Generate {@link String} of executable parameters in brackets in style {@code
     * (getClassName(parameters[0] )arg0, getClassName(parameters[1]) arg1, ... )}
     *
     * @param executable executable to generate {@link String} of
     * @return {@link String} of parameters in brackets with types
     */
    private static String parametersInBracket(Executable executable) {
        Class<?>[] parameters = executable.getParameterTypes();
        return String.format("(%s)", toStringFromIndexWithSeparator(parameters.length,
                i -> String.format("%s arg%d", getClassName(parameters[i]), i), ", "));
    }

    /**
     * Generate {@link String} of executable parameters in brackets in style {@code (arg0, arg1, ... )}
     *
     * @param executable executable to generate {@link String} of
     * @return {@link String} of parameters in brackets without types
     */
    private static String parametersInBracketWithoutType(Executable executable) {
        return String.format("(%s)", toStringFromIndexWithSeparator(executable.getParameterTypes().length,
                i -> "arg" + i, ", "));
    }


    /**
     * Checks that {@code executable} do not have private modifier
     *
     * @param executable -  {@link Executable} to check
     * @return returns true if {@code executable} do not have private modifier
     */
    private static boolean notPrivate(Executable executable) {
        return !Modifier.isPrivate(executable.getModifiers());
    }

    /**
     * Generates implementation of methods that return by {@code generateAllMethods(aClass)}
     *
     * @param aClass class which abstract not realised methods should implementation will generate
     * @return {@link String} of generated implementations, each method separate by line
     */
    private static String generateAllMethods(Class<?> aClass) {
        List<Method> methods = getAllInheritedMethods(aClass);
        return methods.stream()
                .map(s -> String.format("%s%s%n%s", tabulation(1), "@Override", generateMethod(s)))
                .collect(Collectors.joining(SEPARATOR.repeat(2)));

    }

    /**
     * Generate constructors for basic {@code aClass} implementation.
     * If {@code aClass} is interface, generate only default constructor.
     * If {@code aClass} is class, generate constructors that have same signature like all {@code aClass} non-private
     * constructors and passes arguments to {@code super}
     *
     * @param aClass class in each constructor parameters pass
     * @return {@link String} of implementation of constructors
     * @throws ImplerException throws when {@code aClass} have only private constructors
     */
    private static String generateAllConstructors(Class<?> aClass) throws ImplerException {
        if (aClass.isInterface()) {
            return "";
        }
        String answer = Arrays.stream(aClass.getDeclaredConstructors())
                .filter(Implementor::notPrivate)
                .map(Implementor::generateConstructor)
                .collect(Collectors.joining(SEPARATOR.repeat(2)));
        if (answer.isEmpty()) {
            throw new ImplerException("Only private constructors in: " + aClass.getCanonicalName());
        }
        return answer;
    }


    /**
     * Cover of {@link Method} that implement equals by signature
     */
    private record MethodSignature(Method method) {

        /**
         * Checks that signature of {@code first} and {@code second} are equal
         *
         * @param first  - first method to compare
         * @param second - second method to compare
         * @return - return true if methods have equal signature
         */
        private static boolean equalSignature(Method first, Method second) {
            if (first.getParameterCount() != second.getParameterCount() || !first.getName().equals(second.getName())) {
                return false;
            }
            var aTypes = first.getParameterTypes();
            var bTypes = second.getParameterTypes();
            return IntStream.range(0, first.getParameterCount())
                    .allMatch(i -> (aTypes[i].getTypeName().equals(bTypes[i].getTypeName())));
        }

        /**
         * Generate hash code of {@link MethodSignature}
         *
         * @return hash code is equal to {@code method().getName().hashCode()}
         */
        @Override
        public int hashCode() {
            return method.getName().hashCode();
        }

        /**
         * Check that {@code this} and {@code o} have same signature.
         *
         * @param o {@link MethodSignature} to compare.
         * @return false if {@code o} is not {@link MethodSignature}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
            return equalSignature(method, that.method);
        }
    }

    /**
     * Finds all methods that are not realised in {@code aClass} superclasses and interfaces.
     *
     * @param aClass class to get all methods of
     * @return list of all methods that are not realised in {@code aClass} superclasses and interfaces.
     */
    private static List<Method> getAllInheritedMethods(Class<?> aClass) {
        return Stream.concat(Arrays.stream(aClass.getMethods()),
                        Stream.<Class<?>>iterate(aClass,
                                        s -> s != null && Modifier.isAbstract(s.getModifiers()),
                                        Class::getSuperclass)
                                .flatMap(s -> Arrays.stream(s.getDeclaredMethods()))
                                .filter(Implementor::notPrivate))
                .map(MethodSignature::new)
                .distinct()
                .map(MethodSignature::method)
                .filter(s -> Modifier.isAbstract(s.getModifiers()))
                .toList();

    }

}

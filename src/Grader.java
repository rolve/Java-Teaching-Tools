import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;

public class Grader {

    private static final Pattern lines = Pattern.compile("\r?\n");

    @SuppressWarnings("serial")
	private static final List<Task> TASKS = new ArrayList<Task>() {{
        add(new Task("u04", StringAdditionGradingTest.class, 99999 * 3/5));
    }};

    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args[0]);
        new Grader(root).run();
    }

    private Path root;
    private Map<Task, Results> results;

    public Grader(Path root) {
        this.root = root;
        results = TASKS.stream().collect(toMap(t -> t, t -> new Results()));
    }

    private void run() throws IOException {
        List<Path> solutions = Files.list(root)
                .filter(Files::isDirectory)
                //.filter(s -> s.getFileName().toString().startsWith("anarnold"))
                .sorted()
                .collect(toList());
        
        for (int i = 0; i < solutions.size(); i++) {
        	Path solution = solutions.get(i);
            System.out.println("Grading " + solution.getFileName() + " " + (i+1) + "/" + solutions.size());
			grade(solution);
			System.out.println();
        }

        for (Entry<Task, Results> entry : results.entrySet()) {
            entry.getValue().writeTo(Paths.get(entry.getKey().resultFileName()));
        }
    }

    private void grade(Path solution) throws IOException {
        for (Task task : TASKS) {
            gradeTask(solution, task);
        }
    }

    private void gradeTask(Path solution, Task task) throws IOException {
        Path projectPath = solution.resolve(task.projectName);
        String student = solution.getFileName().toString();

        results.get(task).addStudent(student);
        boolean compiled = compileProject(projectPath);
        if (compiled) {
            results.get(task).addCriterion(student, "compiles");

            runTests(task, projectPath, student);
        }
    }

    private boolean compileProject(Path projectPath) throws IOException {
        String classpath = Paths.get("lib", "junit.jar").toAbsolutePath() + File.pathSeparator +
                Paths.get("lib","hamcrest.jar").toAbsolutePath() + File.pathSeparator +
                Paths.get("inspector.jar").toAbsolutePath();
        
        Files.createDirectories(projectPath.resolve("bin"));
        List<String> builderArgs = new ArrayList<>(Arrays.asList("javac", "-d", "bin", "-encoding", "UTF8", "-cp", classpath));
        
        Set<String> files = Files.list(projectPath.resolve("src"))
        		.map(Path::toString)
        		.filter(f -> f.endsWith(".java"))
        		.collect(Collectors.toSet());
		
        while(true) {
        	ArrayList<String> args = new ArrayList<>(builderArgs);
	        args.addAll(files);
	        
	        Process javac = new ProcessBuilder(args)
	        		.redirectErrorStream(true)
	                .directory(projectPath.toFile())
	                .start();
	        
	        StringWriter writer = new StringWriter();
			new LineCopier(javac.getInputStream(), new LineWriterAdapter(writer)).call();
			
			if (robustWaitFor(javac) == 0)
				return true;
			 
			String err = writer.toString();
			Pattern errorPattern = Pattern.compile("/.*/([^/]+\\.java):\\d+: error:",
					Pattern.CASE_INSENSITIVE);
			
			Matcher matcher = errorPattern.matcher(err);
			if (matcher.find()) {
				String faultyFile = projectPath
						.resolve("src")
						.resolve(matcher.group(1))
						.toString();
				files.remove(faultyFile);
				
				System.err.printf("%s appears to be faulty. Ignoring it for compilation.\n", faultyFile);
			} else {
				System.err.println(err);
				break;
			}
        }
        
		return false;
    }

    private void runTests(Task task, Path projectPath, String student) throws IOException {
        List<String> classes = Files.list(projectPath.resolve("src"))
                .map(p -> p.getFileName().toString())
                .filter(s -> s.endsWith(".java"))
                .map(s -> s.substring(0, s.length() - 5))
                .collect(toList());
        String agentArg = "-javaagent:inspector.jar=" + task.instrThreshold + "," +
                classes.stream().collect(joining(","));

        List<String> junitArgs = new ArrayList<>(classes);
        junitArgs.add(0, task.testClass.getName());
        JavaProcessBuilder jUnitBuilder = new JavaProcessBuilder(TestRunner.class, junitArgs);
        jUnitBuilder.classpath(projectPath.resolve("bin") + File.pathSeparator + jUnitBuilder.classpath())
                .vmArgs("-Dfile.encoding=UTF8", agentArg, "-XX:-OmitStackTraceInFastThrow");

        Process jUnit = jUnitBuilder.build()
                .redirectError(Redirect.INHERIT)
                .start();

        StringWriter jUnitOutput = new StringWriter();
        new LineCopier(jUnit.getInputStream(), new LineWriterAdapter(jUnitOutput)).run();

        lines.splitAsStream(jUnitOutput.toString()).forEach(line -> results.get(task).addCriterion(student, line));
    }

    private int robustWaitFor(Process javac) {
        int returnCode;
        while (true) {
            try {
                returnCode = javac.waitFor();
                break;
            } catch(InterruptedException e) {}
        }
        return returnCode;
    }
}

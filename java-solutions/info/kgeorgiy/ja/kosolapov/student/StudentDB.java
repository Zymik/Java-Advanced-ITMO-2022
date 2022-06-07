package info.kgeorgiy.ja.kosolapov.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.GroupQuery;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.Group;


import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements GroupQuery {

    private static final Comparator<Student> NAME_COMPARATOR =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .reversed()
                    .thenComparing(Student::getId);

    private static final Comparator<Student> ID_COMPARATOR = Comparator.comparingInt(Student::getId);

    private static Map<GroupName, List<Student>> mapByGroupName(Collection<Student> students) {
        return students
                .stream()
                .collect(Collectors.groupingBy(Student::getGroup));
    }


    private static List<Group> getGroupByOrder(Collection<Student> students, Comparator<? super Student> order) {
        return mapByGroupName(students)
                .entrySet()
                .stream()
                .map(entry -> new Group(entry.getKey(),
                        entry.getValue().stream().sorted(order).toList()))
                .sorted(Comparator.comparing(Group::getName))
                .toList();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupByOrder(students, NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupByOrder(students, ID_COMPARATOR);
    }

    private static <T> GroupName getLargestGroupByComparator(Collection<Student> students,
                                                             Function<Map.Entry<GroupName, List<Student>>,
                                                                     Map.Entry<GroupName, T>> mapper,
                                                             Comparator<Map.Entry<GroupName, T>> comparator) {
        return mapByGroupName(students)
                .entrySet()
                .stream()
                .map(mapper)
                .max(comparator)
                .map(Map.Entry::getKey)
                .orElse(null);

    }

    private static final Function<Map.Entry<GroupName, ?>, String> GET_NAME = x -> x.getKey().name();

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroupByComparator(students,
                        x -> Map.entry(x.getKey(), x.getValue().size()),
                Map.Entry.<GroupName, Integer>comparingByValue()
                        .thenComparing(GET_NAME));
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupByComparator(students,
                x -> Map.entry(x.getKey(),
                        x.getValue()
                                .stream()
                                .map(Student::getFirstName)
                                .distinct()
                                .count()),
                Map.Entry.<GroupName, Long>comparingByValue()
                        .thenComparing(GET_NAME,
                                Comparator.reverseOrder()));
    }

    private static <T> List<T> getStudentsProperty(List<Student> students, Function<Student, T> getter) {
        return students
                .stream()
                .map(getter)
                .toList();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getStudentsProperty(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getStudentsProperty(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getStudentsProperty(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getStudentsProperty(students, s -> s.getFirstName() + " " + s.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students
                .stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students
                .stream()
                .max(ID_COMPARATOR)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students
                .stream()
                .sorted(ID_COMPARATOR)
                .toList();
    }


    private static List<Student> sortStudentsByName(Stream<Student> stream) {
        return stream.sorted(NAME_COMPARATOR).toList();
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsByName(students.stream());

    }

    private List<Student> findStudentsByPredicateSortedByName
            (Collection<Student> students, Predicate<? super Student> predicate) {
        return sortStudentsByName(students
                .stream()
                .filter(predicate));
    }

    private <T> List<Student> findStudentsByFieldSortedByName(Collection<Student> students,
                                                              Function<Student, T> getter, T fieldValue) {
        return findStudentsByPredicateSortedByName(students, s -> getter.apply(s).equals(fieldValue));

    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsByFieldSortedByName(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsByFieldSortedByName(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByFieldSortedByName(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students
                .stream()
                .filter(s -> s.getGroup() == group)
                .collect(Collectors.toUnmodifiableMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())
                ));
    }
}

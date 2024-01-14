import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Data
public class Person implements Comparable<Person> {

    private int id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Person> peopleMet = new HashSet<>();

    public Person(int id) {
        this.id = id;

    }

    public boolean hasMet(Person person) {
        return this.peopleMet.contains(person);
    }

    public boolean hasNotMet(Person person) {
        return !hasMet(person);
    }

    public void meet(Person person) {
        this.peopleMet.add(person);
    }

    public void clearPeopleMet(){
        peopleMet.clear();
    }

    @Override
    public int compareTo(Person person) {
        Integer countPeopleMet = this.getPeopleMet().size();
        return countPeopleMet.compareTo(person.getPeopleMet().size());
    }
}

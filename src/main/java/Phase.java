import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
public class Phase extends ArrayList<Room> {

    private final List<Person> totalPeopleInPhaseToDistribute;

    private final Set<Person> peopleDistributed = new HashSet<>();

    private final Integer maxRoomSize;

    public Phase(List<Person> people, Integer maxRoomSize) {
        List<Person> totalPeople = new ArrayList<>(people);
        Collections.sort(totalPeople);
        this.totalPeopleInPhaseToDistribute = ListUtils.unmodifiableList(totalPeople);
        this.maxRoomSize = maxRoomSize;
    }

    public List<Person> getPeopleToDistribute() {
        return totalPeopleInPhaseToDistribute.stream()
                .filter(Predicate.not(peopleDistributed::contains))
                .sorted()
                .collect(Collectors.toList());
    }

    public boolean isFinished() {
        return this.totalPeopleInPhaseToDistribute.size() == this.peopleDistributed.size();
    }

    public boolean isNotFinished() {
        return !isFinished();
    }

    public boolean isDistributed(Person person) {
        return this.peopleDistributed.contains(person);
    }

    public Optional<Person> getUnknownNotDistributedPersonFor(Person person) {
        return this.getPeopleToDistribute().stream().filter(person1 -> person.hasNotMet(person)).findAny();
    }

    public Optional<Room> getAnyBiggestRoom() {
        return this.stream().max(Comparator.comparing(Room::size)).stream().findAny();
    }

    public Optional<Room> getFittingRoom(Room room){
        return getAnyRoomOrSmaller(getMaxRoomSize() - room.size());
    }

    public Optional<Room> getAnyRoom(int size) {
        return this.stream().filter(room -> room.size() == size).findAny();
    }

    public Optional<Room> getAnyRoomOrSmaller(int size) {
        Optional<Room> anyRoomOfSize = getAnyRoom(size);
        return anyRoomOfSize.isPresent() ? anyRoomOfSize : this.stream().filter(room -> room.size() < size).findAny();
    }
}

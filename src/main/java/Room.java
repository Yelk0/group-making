import lombok.Data;
import lombok.ToString;

import java.util.*;

@Data
public class Room extends HashSet<Person> implements Comparable<Room> {

    private final Integer id;

    @ToString.Exclude
    private final Set<Person> peopleTryingToEnterTheRoom = new HashSet<>();

    @ToString.Exclude
    private final Integer maxPeopleTryingToEnterRoom;

    private final Integer maxRoomSize;

    private final Phase phase;

    public Room(Integer id, Phase phase) {
        this.id = id;
        this.phase = phase;
        this.maxRoomSize = phase.getMaxRoomSize();
        this.maxPeopleTryingToEnterRoom = phase.getTotalPeopleInPhaseToDistribute().size();
    }

    @Override
    public boolean add(Person person) {
        peopleTryingToEnterTheRoom.add(person);
        if (isFull() || hasAnyoneMet(person) || phase.isDistributed(person)) {
            return false;
        }

        phase.getPeopleDistributed().add(person);
        return super.add(person);
    }

    public boolean addAllForced(Collection<Person> people) {
        boolean modified = false;
        for (Person person : people)
            if (addForced(person)) {
                modified = true;
            }
        return modified;
    }

    public boolean addForced(Person person) {
        peopleTryingToEnterTheRoom.add(person);
        phase.getPeopleDistributed().add(person);
        return super.add(person);
    }

    public boolean isMaxTriesReached() {
        return this.maxPeopleTryingToEnterRoom <= this.peopleTryingToEnterTheRoom.size();
    }

    public boolean hasAnyoneMet(Person person) {
        Iterator<Person> iterator = this.iterator();
        while (iterator.hasNext()) {
            Person roomMember = iterator.next();
            if (roomMember.hasMet(person)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEveryoneMet(Person person) {
        return !hasSomebodyNotMet(person);
    }

    public boolean hasSomebodyNotMet(Person person) {
        return this.stream().anyMatch(personInRoom -> person !=personInRoom && personInRoom.hasNotMet(person));
    }

    public boolean isFull() {
        return this.size() >= maxRoomSize;
    }

    public boolean isNotFull() {
        return !isFull();
    }

    public boolean isNotPossibleToEnter() {
        return isFull() || isMaxTriesReached() || phase.isFinished();
    }

    public Integer countUnknownPeople(Person person) {
        int counter = 0;
        Iterator<Person> iterator = this.iterator();
        while (iterator.hasNext()) {
            Person roomMember = iterator.next();
            if (roomMember.hasNotMet(person)) {
                counter++;
            }
        }
        return counter;
    }

    public boolean hasPotentialNewRelationships(){
        return this.stream().anyMatch(this::hasSomebodyNotMet);
    }

    public boolean hasNoPotentialNewRelationships(){
        return !hasPotentialNewRelationships();
    }

    @Override
    public int compareTo(Room room) {
        Integer peopleInRoom = this.size();
        return peopleInRoom.compareTo(room.size());
    }
}

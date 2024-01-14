import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MatchingService {

    public List<Person> createPeople(Integer headcount) {
        List<Person> people = new ArrayList<>();
        for (int cont = 0; cont < headcount; cont++) {
            people.add(new Person(cont));
        }
        return people;
    }

    @Deprecated
    public List<Phase> getMatchingPhases(List<Person> people, Integer maxRoomsAvailable) {
        var maxHeadcountPerRoom = (int) Math.ceil(people.size() / Double.valueOf(maxRoomsAvailable));
        List<Phase> phases = new ArrayList<>();
        int roomCount = 0;

        while (!allSeen(people)) {
            Phase phase = new Phase(people, maxHeadcountPerRoom);
            Room room = new Room(roomCount++, phase);
            phase.add(room);

            while (phase.isNotFinished()) {
                for (Person person : phase.getTotalPeopleInPhaseToDistribute()) {
                    if (phase.isFinished() || room.isFull()) {
                        break;
                    }

                    room.add(person);
                }

                if (room.isNotPossibleToEnter() && phase.isNotFinished() && phase.size() < maxRoomsAvailable) {
                    room = new Room(roomCount++, phase);
                    phase.add(room);
                }

                if (room.isNotPossibleToEnter() && phase.isNotFinished() && phase.size() >= maxRoomsAvailable) {
                    break;
                }
            }

            optimiseRoomsIfPossible(phase);

            distributeRemainingPeople(phase);

            meetEachOther(phase);
            phases.add(phase);
        }


        return phases;
    }

    public List<Phase> getRoundRobinPhases(List<Person> people, Integer maxRoomsAvailable) {
        List<Phase> phases = new ArrayList<>();

        List<List<Person>> swiftingRows = getSwiftingRows(people, maxRoomsAvailable);

        var maxHeadcountPerRoom = (int) Math.ceil(people.size() / Double.valueOf(maxRoomsAvailable));
        List<Phase> initialPhases = getInitialPhases(people, swiftingRows, maxHeadcountPerRoom, maxRoomsAvailable);
        phases.addAll(initialPhases);

        List<Phase> roundRobinPhases = getRoundRobinPhases(people, maxRoomsAvailable, swiftingRows, maxHeadcountPerRoom);
        phases.addAll(roundRobinPhases);

        for (Phase phase : phases) {
            meetEachOther(phase);
        }

        return phases;
    }

    private List<Phase> getRoundRobinPhases(List<Person> people, Integer maxRoomsAvailable, List<List<Person>> swiftingRows, int maxHeadcountPerRoom) {
        ArrayList<Phase> phases = new ArrayList<>();
        for (int phaseNumber = 0; phaseNumber < maxRoomsAvailable; phaseNumber++) {
            Phase phase = new Phase(people, maxHeadcountPerRoom);
            phases.add(phase);

            for (int roomNumber = 0; roomNumber < maxRoomsAvailable; roomNumber++) {
                Room room = new Room(roomNumber, phase);
                phase.add(room);
                for (int swiftingRowNumber = 0; swiftingRowNumber < swiftingRows.size(); swiftingRowNumber++) {
                    int index = roomNumber + (phaseNumber * swiftingRowNumber);
                    if (index >= maxRoomsAvailable) {
                        index %= maxRoomsAvailable;
                    }
                    List<Person> swiftingRow = swiftingRows.get(swiftingRowNumber);
                    if (swiftingRow.size() > index) {
                        room.addForced(swiftingRow.get(index));
                    }
                }
            }
        }
        return phases;
    }

    private List<Phase> getInitialPhases(List<Person> people, List<List<Person>> swiftingRows, int maxHeadcountPerRoom, int maxRoomsAvailable) {
        List<Phase> initialPhases = new ArrayList<>();
        Phase initialPhase = new Phase(people, maxHeadcountPerRoom);
        initialPhases.add(initialPhase);
        int roomIdentifier = 0;
        for (int i = 0, swiftingRowsSize = swiftingRows.size(); i < swiftingRowsSize; i++) {
            List<Person> swifting = swiftingRows.get(i);
            if (roomIdentifier >= maxRoomsAvailable) {
                initialPhase = new Phase(people, maxHeadcountPerRoom);
                initialPhases.add(initialPhase);
                roomIdentifier = 0;
            }
            Room room = new Room(roomIdentifier, initialPhase);
            for (Person person : swifting) {
                room.add(person);
            }
            initialPhase.add(room);
            roomIdentifier++;
        }

        for (int j = 0; j < maxRoomsAvailable; j++) {
            List<List<Person>> swiftingRowsInitialPhases = new ArrayList<>();
            for (int i = 0, initialPhasesSize = initialPhases.size(); i < initialPhasesSize; i++) {
                int swiftingRowIndex = j + (maxRoomsAvailable * i);
                if (swiftingRowIndex < swiftingRows.size()) {
                    swiftingRowsInitialPhases.add(swiftingRows.get(swiftingRowIndex));
                }
            }
            if (swiftingRowsInitialPhases.size() > 1) {
                List<Phase> roundRobinPhases = getRoundRobinPhases(people, maxRoomsAvailable, swiftingRowsInitialPhases, maxHeadcountPerRoom);
                List<Phase> optimiseRoundRobin = optimiseRoundRobin(roundRobinPhases, swiftingRowsInitialPhases);
                initialPhases.addAll(optimiseRoundRobin);
            }
        }

        return initialPhases;
    }

    private List<Phase> optimiseRoundRobin(List<Phase> roundRobinPhases, List<List<Person>> swiftingRows) {
        if (optimisible(roundRobinPhases)) {
            List<Phase> phasesOptimised = new ArrayList<>();

            //Assume swiftingRows know eachother
            meetEachOther(swiftingRows);

            for (int i = 0, roundRobinPhasesSize = roundRobinPhases.size(); i < roundRobinPhasesSize; i++) {
                Phase phase = roundRobinPhases.get(i);
                var phaseOptimised = new Phase(phase.getTotalPeopleInPhaseToDistribute(), phase.getMaxRoomSize());
                Optional<Room> biggestRoom = phase.getAnyBiggestRoom();
                while (biggestRoom.isPresent()) {
                    var biggestRoomIdentified = biggestRoom.get();
                    if (biggestRoomIdentified.hasNoPotentialNewRelationships()) {
                        phase.remove(biggestRoomIdentified);
                        biggestRoom = phase.getAnyBiggestRoom();
                        continue;
                    }
                    var roomOptimised = new Room(biggestRoomIdentified.getId(), phase);
                    phaseOptimised.add(roomOptimised);
                    roomOptimised.addAllForced(biggestRoomIdentified);
                    phase.remove(biggestRoomIdentified);
                    var fittingRoom = phase.getFittingRoom(roomOptimised);
                    while (fittingRoom.isPresent()) {
                        var fittingRoomIdentified = fittingRoom.get();
                        roomOptimised.addAllForced(fittingRoomIdentified);
                        phase.remove(fittingRoomIdentified);
                        removePeopleNotMeetingNewPeople(roomOptimised, swiftingRows);
                        fittingRoom = phase.getFittingRoom(roomOptimised);
                    }
                    biggestRoom = phase.getAnyBiggestRoom();
                }
                var phaseOptimal = new Phase(phase.getTotalPeopleInPhaseToDistribute(), phase.getMaxRoomSize());
                List<Room> optimalRooms = phaseOptimised.stream()
                        .filter(room -> arePartOfDifferentSwiftingRows(room, swiftingRows))
                        .collect(Collectors.toList());
                phaseOptimal.addAll(optimalRooms);
                meetEachOther(phaseOptimal);
                phasesOptimised.add(phaseOptimal);
            }
            clearPeopleMet(phasesOptimised.get(0).getTotalPeopleInPhaseToDistribute());
            return phasesOptimised;
        }

        return roundRobinPhases;
    }

    private void removePeopleNotMeetingNewPeople(Room room, List<List<Person>> swiftingRows) {
        List<Person> peopleToBeRemoved = new ArrayList<>();
        List<Person> totalPeople = swiftingRows.stream().flatMap(List::stream).collect(Collectors.toList());
        for (Person personInRoom : room) {
            boolean hasMetEveryone = true;
            for (Person person : totalPeople) {
                if (personInRoom != person && personInRoom.hasNotMet(person)) {
                    hasMetEveryone = false;
                }
            }
            if (hasMetEveryone) {
                peopleToBeRemoved.add(personInRoom);
            }
        }
        swiftingRows.removeAll(peopleToBeRemoved);
    }

    private boolean arePartOfDifferentSwiftingRows(Room room, List<List<Person>> swiftingRows) {
        List<Person> swiftingRowOfMember = null;
        for (Person person : room) {
            for (List<Person> swiftingRow : swiftingRows) {
                if (swiftingRow.contains(person)) {
                    if (swiftingRowOfMember == null) {
                        swiftingRowOfMember = swiftingRow;
                    } else if (swiftingRowOfMember != swiftingRow) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    private boolean optimisible(List<Phase> phases) {
        return phases.stream().flatMap(Phase::stream).anyMatch(phase -> phase.size() < phase.getMaxRoomSize() / 2);
    }

    private List<List<Person>> getSwiftingRows(List<Person> people, Integer maxRoomsAvailable) {
        int counter = 1;
        List<List<Person>> swiftingRows = new ArrayList<>();
        List<Person> row = new ArrayList<>();
        swiftingRows.add(row);
        for (Person person : people) {
            row.add(person);
            if (counter % maxRoomsAvailable == 0 && people.get(people.size() - 1) != person) {
                row = new ArrayList<>();
                swiftingRows.add(row);
            }
            counter++;
        }

        return swiftingRows;
    }

    private void distributeRemainingPeople(Phase phase) {
        for (Person person : phase.getPeopleToDistribute()) {
            Room roomWithMorePeopleUnknown = getRoomWithMorePeopleUnknown(phase, person);
            if (roomWithMorePeopleUnknown.countUnknownPeople(person) == 0) {
                Optional<Person> unknownPerson = phase.getUnknownNotDistributedPersonFor(person);
                if (unknownPerson.isPresent()) {
                    Optional<Room> perfectRoom = phase.stream()
                            .filter(room -> room.size() + 2 == phase.getMaxRoomSize())
                            .findAny();
                    if (perfectRoom.isPresent()) {
                        perfectRoom.get().addForced(person);
                        perfectRoom.get().addForced(unknownPerson.get());
                    } else {
                        Optional<Room> anyRoomWithSize = phase.stream().filter(room -> room.size() + 2 < phase.getMaxRoomSize())
                                .findAny();
                        if (anyRoomWithSize.isPresent()) {
                            anyRoomWithSize.get().addForced(person);
                            anyRoomWithSize.get().addForced(unknownPerson.get());
                        } else {
                            Optional<Room> lastChance = phase.stream().filter(room -> room.size() + 1 == phase.getMaxRoomSize())
                                    .findAny();
                            if (lastChance.isPresent()) {
                                lastChance.get().addForced(person);
                            } else {
                                throw new RuntimeException("This should be impossible to happen");
                            }
                        }
                    }
                }
            } else {
                roomWithMorePeopleUnknown.addForced(person);
            }
        }
    }

    private void optimiseRoomsIfPossible(Phase phase) {
        boolean potentiallyOptimisable = false;
        List<Room> roomsNonEmpty = phase.stream().filter(Predicate.not(Room::isEmpty)).toList();
        Room roomWithLessPeople = phase.stream()
                .filter(Predicate.not(Room::isEmpty))
                .min(Room::compareTo).get();
        for (Room room : roomsNonEmpty) {
            if (room != roomWithLessPeople && room.size() + roomWithLessPeople.size() <= phase.getMaxRoomSize()) {
                room.addAllForced(roomWithLessPeople);
                roomWithLessPeople.clear();
                potentiallyOptimisable = true;
            }
        }
        if (potentiallyOptimisable) {
            optimiseRoomsIfPossible(phase);
        }
    }

    private Room getRoomWithMorePeopleUnknown(Phase phase, Person person) {
        Room roomSelected = phase.stream().filter(Room::isNotFull)
                .max(Comparator.comparing(createdRoom -> createdRoom.countUnknownPeople(person)))
                .get();

        return roomSelected;
    }

    private void clearPeopleMet(List<Person> people) {
        for (Person person : people) {
            person.clearPeopleMet();
        }
    }

    private void meetEachOther(Phase phase) {
        for (Room room : phase) {
            for (Person person : room) {
                for (Person person2 : room) {
                    if (person != person2) {
                        person.meet(person2);
                    }
                }
            }
        }
    }

    private void meetEachOther(List<List<Person>> swiftingRows) {
        for (List<Person> people : swiftingRows) {
            for (Person person : people) {
                for (Person person2 : people) {
                    if (person != person2) {
                        person.meet(person2);
                    }
                }
            }
        }
    }

    public boolean allSeen(List<Person> people) {
        for (Person person :
                people) {
            for (Person person2 :
                    people) {
                if (person == person2) {
                    continue;
                }
                if (!person.hasMet(person2)) {
                    return false;
                }
            }
        }
        return true;
    }
}

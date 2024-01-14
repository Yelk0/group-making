import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MatchingServiceTest {

    private MatchingService matchingService = new MatchingService();

    @Test
    public void testHappyCase() {
        List<Person> people = matchingService.createPeople(4);
        List<Phase> phases = matchingService.getRoundRobinPhases(people, 2);
        assertThat(matchingService.allSeen(people), is(true));
        for (Phase phase : phases) {
            assertThat(phase.size(), is(2));
        }
        assertThat(phases.size(), is(3));
        for (Person person : people) {
            assertThat(person.getPeopleMet().size(), is(3));
        }
    }

//    @Test
    public void testHappyCaseWithMorePeople() {
        List<Person> people = matchingService.createPeople(25);
        List<Phase> phases = matchingService.getMatchingPhases(people, 5);
        for (Phase phase : phases) {
            assertThat(phase.size(), is(5));
        }
        assertThat(phases.size(), is(6));
        for (Person person : people) {
            assertThat(person.getPeopleMet().size(), is(24));
        }
    }

    @Test
    public void testIndivisibleNumberOfPeople() {
        List<Person> people = matchingService.createPeople(5);
        List<Phase> phases = matchingService.getRoundRobinPhases(people, 2);
        assertThat(matchingService.allSeen(people), is(true));
        for (Phase phase : phases) {
            assertThat(phase.size(), is(3));
        }
        assertThat(phases.size(), is(3));
        for (Person person : people) {
            assertThat(person.getPeopleMet().size(), is(4));
        }
    }

    @Test
    public void testRoundRobin() {
        List<Person> people = matchingService.createPeople(25);
        List<Phase> phases = matchingService.getRoundRobinPhases(people, 5);
        assertThat(matchingService.allSeen(people), is(true));
        for (Phase phase : phases) {
            assertThat(phase.size(), is(5));
        }
        assertThat(phases.size(), is(6));
        for (Person person : people) {
            assertThat(person.getPeopleMet().size(), is(24));
        }
    }

    @Test
    public void testRoundRobinUnevenRooms() {
        List<Person> people = matchingService.createPeople(27);
        List<Phase> phases = matchingService.getRoundRobinPhases(people, 5);
        assertThat(matchingService.allSeen(people), is(true));
        for (Phase phase : phases) {
            assertThat(phase.size(), is(5));
        }
        assertThat(phases.size(), is(6));
        for (Person person : people) {
            assertThat(person.getPeopleMet().size(), is(26));
        }
    }
}
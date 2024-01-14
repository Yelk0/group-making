import java.util.List;

public class Main {

    public static void main(String[] args) {
        Integer maxRoomsAvailable;
        Integer headcount;
        try {
            maxRoomsAvailable = Integer.parseInt(args[0]);
            headcount = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Invalid input provided");
            return;
        }

        MatchingService matchingService = new MatchingService();
        List<Person> people = matchingService.createPeople(headcount);
        matchingService.getRoundRobinPhases(people, maxRoomsAvailable);

    }
}

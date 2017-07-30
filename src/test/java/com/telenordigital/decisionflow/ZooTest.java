package com.telenordigital.decisionflow;

import com.telenordigital.decisionflow.describers.JsonDescriber;
import com.telenordigital.decisionflow.describers.Papyrus;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class ZooTest {

    private final DecisionFlow<AnimalDescription, Animal> theFlow;

    public ZooTest(DecisionFlowDescriber describer) {
        this.theFlow = DecisionFlow.getInstance(describer);
    }

    @Parameters
    public static List<DecisionFlowDescriber> describersToTest() {
        return Arrays.asList(getPapyrusDescriber(), getJsonDescriber());
    }

    enum Environment {WATER, LAND}
    enum AnimalClass {MAMMAL, BIRD, OTHER}
    enum AnimalOrder {PRIMATE, RODENT, OTHER}
    enum Animal {GORILLA, COBRA, HUMAN, OSTRICH, TARANTULA, SEAGULL,
        BLUE_WHALE, WHALE_SHARK, RAT, EAGLE, LEMUR, ELEPHANT, BAT, TIGER, PENGUIN, COCKATOO}

    static class AnimalDescription {
        private Environment environment;
        private AnimalClass animalClass;
        private AnimalOrder animalOrder;
        private int weight;
        private MotionAbilities motionAbilities;
        private IntellectualAbilities intellectualAbilities;
        public AnimalDescription(
                Environment environment,
                AnimalClass animalClass,
                AnimalOrder animalOrder,
                int weight,
                boolean canWalk,
                boolean canDive,
                boolean canFly,
                boolean canTalk,
                boolean canRead,
                boolean canCount
                ) {
            super();
            this.environment = environment;
            this.animalClass = animalClass;
            this.animalOrder = animalOrder;
            this.weight = weight;
            this.motionAbilities = new MotionAbilities(canWalk, canDive, canFly);
            this.intellectualAbilities = new IntellectualAbilities(canTalk, canRead, canCount);
        }
        public Environment getEnvironment() {
            return environment;
        }
        public AnimalClass getAnimalClass() {
            return animalClass;
        }
        public AnimalOrder getAnimalOrder() {
            return animalOrder;
        }
        public int getWeight() {
            return weight;
        }
        public MotionAbilities getMotionAbilities() {
            return motionAbilities;
        }
        public IntellectualAbilities getIntellectualAbilities() {
            return intellectualAbilities;
        }

        // static helpers
        public static AnimalClass getAnimalClass(String value) {
            return AnimalClass.valueOf(value);
        }
        public static AnimalOrder getAnimalOrder(String value) {
            return AnimalOrder.valueOf(value);
        }
        public static Environment getEnvironment(String value) {
            return Environment.valueOf(value);
        }
        public static Animal getAnimal(String value) {
            return Animal.valueOf(value);
        }
    }

    static class MotionAbilities {
        public boolean canWalk;
        public boolean canDive;
        public boolean canFly;
        MotionAbilities(boolean canWalk, boolean canDive, boolean canFly) {
            this.canWalk = canWalk;
            this.canDive = canDive;
            this.canFly = canFly;
        }
    }

    static class IntellectualAbilities {
        public boolean canTalk;
        public boolean canRead;
        public boolean canCount;
        IntellectualAbilities(boolean canTalk, boolean canRead, boolean canCount) {
            this.canTalk = canTalk;
            this.canRead = canRead;
            this.canCount = canCount;
        }
    }

    static final DecisionFlowDescriber ZOO_PAPIRUS_DESCRIBER = Papyrus.getInstance(
            "src/test/resources/papyrus/workspace/zoo/zoo.uml");

    public static DecisionFlowDescriber getJsonDescriber() {
        return JsonDescriber.getInstance(ZOO_PAPIRUS_DESCRIBER);
    }

    public static DecisionFlowDescriber getPapyrusDescriber() {
        return ZOO_PAPIRUS_DESCRIBER;
    }

    @Test
    public void testDeadEnd() {
        AnimalDescription deadEndDescription =
                new AnimalDescription(
                        Environment.WATER, AnimalClass.MAMMAL, AnimalOrder.OTHER,
                        200,
                        false, false, false, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(deadEndDescription);
        assertThat(decision, equalTo(null));
        List<Decision<Animal>> decisions = theFlow.getDecisions(deadEndDescription);
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).isEmpty(),
                equalTo(true));
    }

    @Test
    public void testBlueWhaleAndWhaleShark() {
        AnimalDescription blueWhaleAndWhaleShark =
                new AnimalDescription(
                        Environment.WATER, AnimalClass.MAMMAL, AnimalOrder.OTHER,
                        50000,
                        false, false, false, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(blueWhaleAndWhaleShark);
        assertThat(decision.getPayload(), equalTo(Animal.BLUE_WHALE));
        List<Decision<Animal>> decisions = theFlow.getDecisions(blueWhaleAndWhaleShark);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.BLUE_WHALE, Animal.WHALE_SHARK)),
                equalTo(true));
    }

    @Test
    public void testCobraAndTarantula() {
        AnimalDescription cobraAndTarantula =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.OTHER, AnimalOrder.OTHER,
                        0,
                        false, false, false, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(cobraAndTarantula);
        assertThat(decision.getPayload(), equalTo(Animal.COBRA));
        assertThat(decision.getAttributes().get("legCount"), equalTo(null));
        assertThat(decision.getAttributes().get("description"),
                equalTo("One of the most feared snakes"));
        List<Decision<Animal>> decisions = theFlow.getDecisions(cobraAndTarantula);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.COBRA, Animal.TARANTULA)),
                equalTo(true));
    }

    @Test
    public void testGorillaAndLemur() {
        AnimalDescription gorillaAndLemur =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.MAMMAL, AnimalOrder.PRIMATE,
                        200,
                        true, false, false, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(gorillaAndLemur);
        assertThat(decision.getPayload(), equalTo(Animal.GORILLA));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        assertThat(decision.getAttributes().get("description"),
                equalTo(null));
        List<Decision<Animal>> decisions = theFlow.getDecisions(gorillaAndLemur);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.GORILLA, Animal.LEMUR)),
                equalTo(true));
    }

    @Test
    public void testHuman() {
        AnimalDescription human =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.MAMMAL, AnimalOrder.PRIMATE,
                        200,
                        false, false, false, false, true, false);
        Decision<Animal> decision = theFlow.getDecision(human);
        assertThat(decision.getPayload(), equalTo(Animal.HUMAN));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        assertThat(decision.getAttributes().get("description"),
                equalTo("The naked ape"));
        List<Decision<Animal>> decisions = theFlow.getDecisions(human);
        assertThat(decisions.size(), equalTo(1));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.HUMAN)),
                equalTo(true));
    }

    @Test
    public void testRat() {
        AnimalDescription rat =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.MAMMAL, AnimalOrder.RODENT,
                        50000,
                        false, false, false, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(rat);
        assertThat(decision.getPayload(), equalTo(Animal.RAT));
        assertThat(decision.getAttributes().get("legCount"), equalTo(4));
        assertThat(decision.getAttributes().get("description"),
                equalTo("They say the most adaptable mammal on Earth"));
        List<Decision<Animal>> decisions = theFlow.getDecisions(rat);
        assertThat(decisions.size(), equalTo(1));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.RAT)),
                equalTo(true));
    }

    @Test
    public void testElephantTigerAndBat() {
        AnimalDescription elephantTigerAndBat =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.MAMMAL, AnimalOrder.OTHER,
                        50000,
                        false, false, false, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(elephantTigerAndBat);
        assertThat(decision.getPayload(), equalTo(Animal.ELEPHANT));
        assertThat(decision.getAttributes().get("legCount"), equalTo(4));
        assertThat(decision.getAttributes().get("trunkCount"), equalTo(1));
        assertThat(decision.getAttributes().get("description"),
                equalTo("Huge, dark and wrinkled, as opposed to an aspirin"));
        List<Decision<Animal>> decisions = theFlow.getDecisions(elephantTigerAndBat);
        assertThat(decisions.size(), equalTo(3));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.ELEPHANT, Animal.TIGER, Animal.BAT)),
                equalTo(true));
    }

    @Test
    public void testOstrich() {
        AnimalDescription ostrich =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.BIRD, AnimalOrder.OTHER,
                        50000,
                        false, true, false, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(ostrich);
        assertThat(decision.getPayload(), equalTo(Animal.OSTRICH));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        assertThat(decision.getAttributes().get("description"),
                equalTo("One of the fastest runners on Earth"));
        List<Decision<Animal>> decisions = theFlow.getDecisions(ostrich);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.OSTRICH, Animal.PENGUIN)),
                equalTo(true));
    }

    @Test
    public void testEagle() {
        AnimalDescription eagle =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.BIRD, AnimalOrder.OTHER,
                        50000,
                        false, false, true, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(eagle);
        assertThat(
                Animal.EAGLE.equals(decision.getPayload())
                || Animal.COCKATOO.equals(decision.getPayload()), equalTo(true));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        List<Decision<Animal>> decisions = theFlow.getDecisions(eagle);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.EAGLE)),
                equalTo(true));
    }

    @Test
    public void testSeagull() {
        AnimalDescription seagull =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.BIRD, AnimalOrder.OTHER,
                        50000,
                        false, true, true, false, false, false);
        Decision<Animal> decision = theFlow.getDecision(seagull);
        assertThat(decision.getPayload(), equalTo(Animal.COCKATOO));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        assertThat(decision.getAttributes().get("description"),
                equalTo("Demonstrating <<always>> stereotype"));
        List<Decision<Animal>> decisions = theFlow.getDecisions(seagull);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(Animal.SEAGULL, Animal.COCKATOO)),
                equalTo(true));
    }

    @Test
    public void testContinueFrom() {
        AnimalDescription elephantTigerAndBat =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.MAMMAL, AnimalOrder.OTHER,
                        50000,
                        false, false, false, false, false, false);

        Decision<Animal> elephant = theFlow.getDecision(elephantTigerAndBat);
        assertThat(elephant.getPayload(), equalTo(Animal.ELEPHANT));

        Decision<Animal> tiger = theFlow.continueFrom(elephant, elephantTigerAndBat);
        assertThat(tiger.getPayload(), equalTo(Animal.TIGER));


        Decision<Animal> bat = theFlow.continueFrom(tiger, elephantTigerAndBat);
        assertThat(bat.getPayload(), equalTo(Animal.BAT));

        Decision<Animal> nullDecision = theFlow.continueFrom(bat, elephantTigerAndBat);
        assertThat(nullDecision, equalTo(null));
    }

    @Test
    public void testContinueFrom2() {
        AnimalDescription elephantTigerAndBat =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.MAMMAL, AnimalOrder.OTHER,
                        50000,
                        false, false, false, false, false, false);

        Decision<Animal> elephant = theFlow.getDecision(elephantTigerAndBat);
        assertThat(elephant.getPayload(), equalTo(Animal.ELEPHANT));

        Decision<Animal> tiger = theFlow.continueFrom(elephant.getId(), elephantTigerAndBat);
        assertThat(tiger.getPayload(), equalTo(Animal.TIGER));


        Decision<Animal> bat = theFlow.continueFrom(tiger.getId(), elephantTigerAndBat);
        assertThat(bat.getPayload(), equalTo(Animal.BAT));

        Decision<Animal> nullDecision = theFlow.continueFrom(bat.getId(), elephantTigerAndBat);
        assertThat(nullDecision, equalTo(null));
    }
}

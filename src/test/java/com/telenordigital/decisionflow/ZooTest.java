package com.telenordigital.decisionflow;

import com.telenordigital.decisionflow.describers.JsonDescriber;
import com.telenordigital.decisionflow.describers.Papyrus;
import com.telenordigital.decisionflow.describers.VisualParadigm;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public ZooTest(DecisionFlow<AnimalDescription, Animal> flow) {
        this.theFlow = flow;
    }

    @Parameters
    public static List<DecisionMachine<AnimalDescription, Animal>> flowsToTest() {
        return Arrays.asList(
                ZOO_PAPYRUS_FLOW,
                ZOO_PAPYRUS_JSON_FLOW,
                ZOO_VISUAL_PARADIGM_FLOW,
                ZOO_VISUAL_PARADIGM_JSON_FLOW
                );
    }

    private static final DecisionFlowDescriber ZOO_PAPYRUS_DESCRIBER = Papyrus.getInstance(
            "src/test/resources/papyrus/workspace/zoo/zoo.uml");

    private static final DecisionFlowDescriber ZOO_PAPYRUS_JSON_DESCRIBER = JsonDescriber.getInstance(
            ZOO_PAPYRUS_DESCRIBER);

    private static DecisionMachine<AnimalDescription, Animal> ZOO_PAPYRUS_FLOW =
            DecisionFlow.getInstance(ZOO_PAPYRUS_DESCRIBER);

    private static DecisionMachine<AnimalDescription, Animal> ZOO_PAPYRUS_JSON_FLOW =
            DecisionFlow.getInstance(ZOO_PAPYRUS_JSON_DESCRIBER);

    private static final DecisionFlowDescriber ZOO_VISUAL_PARADIGM_DESCRIBER = VisualParadigm.getInstance(
            "src/test/resources/visualparadigm/zoo.xmi");

    private static final DecisionFlowDescriber ZOO_VISUAL_PARADIGM_JSON_DESCRIBER = JsonDescriber
            .getInstance(ZOO_VISUAL_PARADIGM_DESCRIBER);

    private static DecisionMachine<AnimalDescription, Animal> ZOO_VISUAL_PARADIGM_FLOW =
            DecisionFlow.getInstance(ZOO_VISUAL_PARADIGM_DESCRIBER);

    private static DecisionMachine<AnimalDescription, Animal> ZOO_VISUAL_PARADIGM_JSON_FLOW =
            DecisionFlow.getInstance(ZOO_VISUAL_PARADIGM_JSON_DESCRIBER);

    enum Environment {WATER, LAND}
    enum AnimalClass {MAMMAL, BIRD, REPTILE, OTHER}
    enum AnimalOrder {PRIMATE, RODENT, OTHER}
    enum AnimalStrain {GORILLA, COBRA, HUMAN, OSTRICH, TARANTULA, SEAGULL,
        BLUE_WHALE, WHALE_SHARK, RAT, EAGLE, LEMUR, ELEPHANT, BAT, TIGER, PENGUIN, COCKATOO,
        CROCODILE, TURTLE, KOMODO_DRAGON}

    static class Animal implements Decision.OnAttributesCallback{
        AnimalStrain animalStrain;

        Map<String, ?> attributes;

        Animal(final AnimalStrain animalStrain) {
            this.animalStrain = animalStrain;
        }

        @Override
        public void onAttributes(final Map<String, ?> attributes) {
            this.attributes = attributes;
        }

        AnimalStrain getAnimalStrain() {
            return animalStrain;
        }

        Map<String, ?> getAttributes() {
            return attributes;
        }
    }

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
            return new Animal(AnimalStrain.valueOf(value));
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
        assertThat(decision.getPayload().getAnimalStrain(), equalTo(AnimalStrain.BLUE_WHALE));
        assertThat(decision.getPayload().getAttributes(), equalTo(decision.getAttributes()));
        List<Decision<Animal>> decisions = theFlow.getDecisions(blueWhaleAndWhaleShark);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.BLUE_WHALE, AnimalStrain.WHALE_SHARK)),
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
        assertThat(decision.getPayload().getAnimalStrain(), equalTo(AnimalStrain.COBRA));
        assertThat(decision.getAttributes().get("legCount"), equalTo(null));
        assertThat(decision.getAttributes().get("description"),
                equalTo("One of the most feared snakes"));
        assertThat(decision.getPayload().getAttributes(), equalTo(decision.getAttributes()));
        List<Decision<Animal>> decisions = theFlow.getDecisions(cobraAndTarantula);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.COBRA, AnimalStrain.TARANTULA)),
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
        assertThat(decision.getPayload().getAnimalStrain(), equalTo(AnimalStrain.GORILLA));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        assertThat(decision.getAttributes().get("description"),
                equalTo(null));
        List<Decision<Animal>> decisions = theFlow.getDecisions(gorillaAndLemur);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.GORILLA, AnimalStrain.LEMUR)),
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
        assertThat(decision.getPayload().getAnimalStrain(), equalTo(AnimalStrain.HUMAN));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        assertThat(decision.getAttributes().get("description"),
                equalTo("The naked ape"));
        assertThat(decision.getPayload().getAttributes(), equalTo(decision.getAttributes()));
        List<Decision<Animal>> decisions = theFlow.getDecisions(human);
        assertThat(decisions.size(), equalTo(1));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.HUMAN)),
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
        assertThat(decision.getPayload().getAnimalStrain(), equalTo(AnimalStrain.RAT));
        assertThat(decision.getAttributes().get("legCount"), equalTo(4));
        assertThat(decision.getAttributes().get("description"),
                equalTo("They say the most adaptable mammal on Earth"));
        assertThat(decision.getPayload().getAttributes(), equalTo(decision.getAttributes()));
        List<Decision<Animal>> decisions = theFlow.getDecisions(rat);
        assertThat(decisions.size(), equalTo(1));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.RAT)),
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
        assertThat(decision.getPayload().getAnimalStrain(), equalTo(AnimalStrain.ELEPHANT));
        assertThat(decision.getAttributes().get("legCount"), equalTo(4));
        assertThat(decision.getAttributes().get("trunkCount"), equalTo(1));
        assertThat(decision.getAttributes().get("description"),
                equalTo("Huge, dark and wrinkled, as opposed to an aspirin"));
        List<Decision<Animal>> decisions = theFlow.getDecisions(elephantTigerAndBat);
        assertThat(decisions.size(), equalTo(3));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.ELEPHANT, AnimalStrain.TIGER, AnimalStrain.BAT)),
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
        assertThat(decision.getPayload().getAnimalStrain(), equalTo(AnimalStrain.OSTRICH));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        assertThat(decision.getAttributes().get("description"),
                equalTo("One of the fastest runners on Earth"));
        assertThat(decision.getPayload().getAttributes(), equalTo(decision.getAttributes()));
        List<Decision<Animal>> decisions = theFlow.getDecisions(ostrich);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.OSTRICH, AnimalStrain.PENGUIN)),
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
                AnimalStrain.EAGLE.equals(decision.getPayload().getAnimalStrain())
                || AnimalStrain.COCKATOO.equals(decision.getPayload().getAnimalStrain()), equalTo(true));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        List<Decision<Animal>> decisions = theFlow.getDecisions(eagle);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.EAGLE)),
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
        assertThat(decision.getPayload().getAnimalStrain(), equalTo(AnimalStrain.COCKATOO));
        assertThat(decision.getAttributes().get("legCount"), equalTo(2));
        assertThat(decision.getAttributes().get("description"),
                equalTo("Demonstrating <<always>> stereotype"));
        List<Decision<Animal>> decisions = theFlow.getDecisions(seagull);
        assertThat(decisions.size(), equalTo(2));
        assertThat(
                decisions.stream().map(d -> d.getPayload().getAnimalStrain()).collect(Collectors.toList()).
                    containsAll(Arrays.asList(AnimalStrain.SEAGULL, AnimalStrain.COCKATOO)),
                equalTo(true));
    }

    @Test
    public void testRandom() {
        AnimalDescription reptile =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.REPTILE, AnimalOrder.OTHER,
                        50,
                        false, true, true, false, false, false);
        Map<AnimalStrain, Integer> captured = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            Decision<Animal> decision = theFlow.getDecision(reptile);
            Animal animal = decision.getPayload();
            // System.out.println(String.format("%3d %s", i, decision.getDecisionPath()));
            Integer soFar = captured.get(animal.getAnimalStrain());
            if (soFar == null) {
                soFar = 0;
            }
            captured.put(animal.getAnimalStrain(), soFar + 1);
        }
        System.out.println(captured);
        assertThat(captured.size() == 3, equalTo(true));
    }

    @Test
    public void testContinueFrom() {
        AnimalDescription elephantTigerAndBat =
                new AnimalDescription(
                        Environment.LAND, AnimalClass.MAMMAL, AnimalOrder.OTHER,
                        50000,
                        false, false, false, false, false, false);

        Decision<Animal> elephant = theFlow.getDecision(elephantTigerAndBat);
        assertThat(elephant.getPayload().getAnimalStrain(), equalTo(AnimalStrain.ELEPHANT));

        Decision<Animal> tiger = theFlow.continueFrom(elephant, elephantTigerAndBat);
        assertThat(tiger.getPayload().getAnimalStrain(), equalTo(AnimalStrain.TIGER));


        Decision<Animal> bat = theFlow.continueFrom(tiger, elephantTigerAndBat);
        assertThat(bat.getPayload().getAnimalStrain(), equalTo(AnimalStrain.BAT));

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
        assertThat(elephant.getPayload().getAnimalStrain(), equalTo(AnimalStrain.ELEPHANT));

        Decision<Animal> tiger = theFlow.continueFrom(elephant.getId(), elephantTigerAndBat);
        assertThat(tiger.getPayload().getAnimalStrain(), equalTo(AnimalStrain.TIGER));


        Decision<Animal> bat = theFlow.continueFrom(tiger.getId(), elephantTigerAndBat);
        assertThat(bat.getPayload().getAnimalStrain(), equalTo(AnimalStrain.BAT));

        Decision<Animal> nullDecision = theFlow.continueFrom(bat.getId(), elephantTigerAndBat);
        assertThat(nullDecision, equalTo(null));
    }
}

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.Future;

/**
 * Created by Brian on 1/30/2017.
 */
public class GUI {
    private JComboBox<String> constructPlayerDropdown() {
        final JComboBox<String> box = new JComboBox<>();

        // add the hard hands (hard 4-21)
        for (int i = 4; i <= 21; i++) {
            box.addItem("Hard " + i);
        }

        // add the soft hands (soft 12-21)
        for (int i = 12; i <= 21; i++) {
            box.addItem("Soft " + i);
        }

        return box;
    }

    private JComboBox<String> constructDealerDropdown() {
        final JComboBox<String> box = new JComboBox<>();

        for (int i = 2; i <= 10; i++) {
            box.addItem(Integer.toString(i));
        }
        box.addItem("Ace");

        return box;
    }

    private void show() {
        final JFrame frame = new JFrame("Blackjack Solver");
        final JComboBox<String> playerBox = constructPlayerDropdown();
        final JComboBox<String> dealerBox = constructDealerDropdown();
        final JCheckBox isPairBox = new JCheckBox();
        final JButton solveButton = new JButton("Solve");
        final JLabel status = new JLabel("Waiting.");

        frame.setMinimumSize(new Dimension(600, 300));
        frame.setLayout(new GridLayout(0, 1));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        final JPanel playerPanel = new JPanel(new FlowLayout());
        playerPanel.add(new JLabel("Player hand:"));
        playerPanel.add(playerBox);
        playerPanel.add(isPairBox);
        playerPanel.add(new JLabel("Pair?"));
        frame.add(playerPanel);

        final JPanel dealerPanel = new JPanel(new FlowLayout());
        dealerPanel.add(new JLabel("Dealer's up card:"));
        dealerPanel.add(dealerBox);
        frame.add(dealerPanel);

        frame.add(solveButton);
        frame.add(status);
        frame.pack();
        frame.setVisible(true);

        final Decider d = new Decider(400, 1.0);
        solveButton.addActionListener((e) -> {
            new Thread(() -> {
                // get the info required to build a scenario
                final String selectedPlayerHand = (String) playerBox.getSelectedItem();
                final int playerValue = Integer.valueOf(selectedPlayerHand.split(" ")[1]);
                final boolean isSoft = selectedPlayerHand.split(" ")[0].equals("Soft");
                final boolean isPair = isPairBox.isSelected();
                final String selectedDealerCard = (String) dealerBox.getSelectedItem();
                final Card dealerCard = selectedDealerCard.equals("Ace")
                        ? Card.ACE
                        : Card.getCardWithValue(Integer.valueOf(selectedDealerCard));

                // build it
                final Scenario scenario = new Scenario();
                scenario.playerValue = playerValue;
                scenario.dealerCard = dealerCard;
                scenario.isPlayerSoft = isSoft;
                scenario.isPair = isPair;

                // solve
                final Future<?> statusUpdater = d.addStatusListener((opsPerSecond) -> {
                    status.setText("solving " + scenario + " (" + opsPerSecond + " hands per second)");
                });

                try {
                    final long startTime = System.nanoTime();
                    final Pair<Decision, Double> p = d.computeBestScenarioResult(scenario, false);
                    final long endTime = System.nanoTime();
                    status.setText(scenario + " best strategy: " + p.get(Decision.class) + " (" + p.get(Double.class) + ")");
                    System.out.println("Computation time: " + (endTime - startTime) / 1000000000.0 + "s");
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    statusUpdater.cancel(true);
                }
            }).start();
        });
    }

    public static void main(String[] args) {
        final GUI gui = new GUI();
        gui.show();
    }
}

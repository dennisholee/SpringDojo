package io.forest.mcp.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.forest.mcp.client.CalculatorClient;
import io.forest.mcp.dto.CalculatorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Route
public class MainView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    public MainView(CalculatorClient calculatorClient) {
        logger.info("MainView initialized");

        Span resultSpan = new Span("Result: -");

        TextField aField = new TextField("a");
        aField.setPlaceholder("Enter integer a");

        TextField bField = new TextField("b");
        bField.setPlaceholder("Enter integer b");

        Button addButton = new Button("Add", e -> {
            String aText = aField.getValue();
            String bText = bField.getValue();
            logger.debug("Add button clicked - raw inputs: a='{}', b='{}'", aText, bText);

            final int a;
            final int b;
            try {
                a = Integer.parseInt(aText.trim());
            } catch (Exception ex) {
                logger.warn("Invalid integer for a: '{}'", aText);
                resultSpan.setText("Error: invalid integer for a");
                return;
            }
            try {
                b = Integer.parseInt(bText.trim());
            } catch (Exception ex) {
                logger.warn("Invalid integer for b: '{}'", bText);
                resultSpan.setText("Error: invalid integer for b");
                return;
            }

            logger.info("Calling CalculatorClient.add with a={}, b={}", a, b);


            CalculatorRequest calculatorRequest = new CalculatorRequest(
                "2.0",
                UUID.randomUUID().toString(),
                "tools/call",
                "add",
                a,
                b
            );

            calculatorClient.callTool(calculatorRequest)
                .subscribe(
                    result -> {
                        logger.info("Received result from CalculatorClient.add: {}", result);
                        getUI().ifPresent(ui -> ui.access(() -> resultSpan.setText("Result: " + result)));
                    },
                    err -> {
                        logger.error("Error calling CalculatorClient.add: a={}, b={}, error={}", a, b, err.toString(), err);
                        getUI().ifPresent(ui -> ui.access(() -> resultSpan.setText("Error: " + err.getMessage())));
                    }
                );
        });

        add(aField, bField, addButton, resultSpan);
    }
}

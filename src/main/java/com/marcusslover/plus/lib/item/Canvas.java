package com.marcusslover.plus.lib.item;

import com.marcusslover.plus.lib.text.Text;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A canvas is a representation of a menu.
 * It is used to customize the menu layout.
 */
@Data
@Accessors(fluent = true, chain = true)
public class Canvas implements InventoryHolder { // Inventory holder to keep track of the inventory.
    // buttons of the canvas
    @Getter(AccessLevel.PACKAGE)
    private final @NotNull List<Button> buttons = new ArrayList<>();

    // means literally nothing but used for some hacky stuff
    @Getter(AccessLevel.PRIVATE)
    private final @NotNull Button hackyButton = Button.create(-1);

    // pages of the canvas
    @Getter(AccessLevel.PRIVATE)
    private final @NotNull Map<UUID, Integer> pages = new HashMap<>();
    private @NotNull Integer rows; // 1-6 (using non-primitive to allow @NotNull for the constructor)

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private @NotNull Menu assosiatedMenu;
    private @Nullable Component title;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private @Nullable Inventory assosiatedInventory = null;
    private @Nullable ClickContext genericClick = null, selfInventory = null;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PRIVATE)
    private PopulatorContext<?> poplatorContext = null;

    /**
     * Set the title of the canvas.
     *
     * @param title the title
     * @return the canvas
     */
    public @NotNull Canvas title(@Nullable Component title) {
        this.title = title;
        return this;
    }

    /**
     * Set the title of the canvas.
     *
     * @param title the title
     * @return the canvas
     */
    public @NotNull Canvas title(@Nullable Text title) {
        if (title != null) {
            this.title = title.comp();
        } else {
            this.title = null;
        }
        return this;
    }

    /**
     * Set the title of the canvas.
     *
     * @param title the title
     * @return the canvas
     */
    public @NotNull Canvas title(@Nullable String title) {
        if (title != null) {
            this.title = Text.of(title).comp();
        } else {
            this.title = null;
        }
        return this;
    }

    /**
     * Craft the inventory.
     *
     * @return the inventory
     */
    @NotNull Inventory craftInventory() {
        if (this.title == null) {
            return Bukkit.createInventory(this, this.rows * 9);
        }
        return Bukkit.createInventory(this, this.rows * 9, this.title);
    }

    /**
     * Set the self inventory action.
     * This action is called when the player clicks on the inventory.
     *
     * @param selfInventory the self inventory action
     * @return the click context associated with the self inventory action
     */
    public @NotNull ClickContext selfInventory(@Nullable Canvas.SelfInventoryClick selfInventory) {
        this.selfInventory = new ClickContext(this, selfInventory);
        return this.selfInventory;
    }

    /**
     * Set the generic click action.
     * This action is called when the player clicks on the inventory.
     *
     * @param genericClick the generic click action
     * @return the click context associated with the generic click action
     */
    public @NotNull ClickContext genericClick(@Nullable Canvas.GenericClick genericClick) {
        this.genericClick = new ClickContext(this, genericClick);
        return this.genericClick;
    }

    /**
     * Add a button to the canvas.
     *
     * @param button      the button
     * @param buttonClick the button click
     * @return the click context associated with the button
     */
    public @NotNull ClickContext button(@NotNull Button button, @Nullable Canvas.ButtonClick buttonClick) {
        this.buttons.add(button);
        ClickContext context = new ClickContext(this, buttonClick);
        button.clickContext(context);
        return context;
    }

    /**
     * Starts the population of the elements on the canvas.
     *
     * @param elements the elements
     * @param <T>      the type of the elements
     * @return the canvas
     */
    @SuppressWarnings("unchecked")
    public <T> @NotNull PopulatorContext<T> populate(@NotNull List<T> elements) {
        this.poplatorContext = new PopulatorContext<>(this, elements);
        return (PopulatorContext<T>) poplatorContext;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Objects.requireNonNull(this.assosiatedInventory);
    }

    /**
     * A button click. This is called when a player clicks on the button.
     */
    @FunctionalInterface
    public interface ButtonClick {
        /**
         * Called when a target clicks on the button.
         *
         * @param target the target
         * @param event  the event
         */
        void onClick(@NotNull Player target, @NotNull Item clicked, @NotNull InventoryClickEvent event, @NotNull Canvas it);
    }

    /**
     * A generic click. This is called when a player clicks on the inventory.
     * This is called before the button click.
     */
    @FunctionalInterface
    public interface GenericClick extends ButtonClick {

    }

    /**
     * A self inventory click. This is called when a player clicks on their inventory.
     */
    @FunctionalInterface
    public interface SelfInventoryClick extends ButtonClick {
    }

    /**
     * Represents the context of the population.
     * It holds the elements and the canvas.
     * It also holds the view strategy.
     *
     * @param <T> the type of the elements
     */
    @Data
    public static class PopulatorContext<T> {
        private final @NotNull Canvas canvas;
        private final @NotNull List<T> elements;
        private @Nullable ViewStrategy viewStrategy = null;

        /*Page manipulation*/
        private @Nullable Button pageForwards = null;
        private @Nullable Button pageBackwards = null;
        private @Nullable Populator<T> populator = null; // internal

        /**
         * Set the view strategy.
         * Works only with the default view strategies.
         *
         * @param defaultViewStrategy the view strategy
         * @return the populator context
         */
        public @NotNull PopulatorContext<T> viewStrategy(@Nullable DefaultViewStrategy defaultViewStrategy) {
            if (defaultViewStrategy == null) {
                this.viewStrategy = null;
                return this;
            }
            this.viewStrategy = defaultViewStrategy.viewStrategy();
            return this;
        }

        /**
         * Set the view strategy.
         * @param viewStrategy A custom view strategy.
         * @return The populator context.
         */
        public @NotNull PopulatorContext<T> viewStrategy(@Nullable ViewStrategy viewStrategy) {
            this.viewStrategy = viewStrategy;
            return this;
        }

        @SuppressWarnings("unchecked")
        public void updateContent(@NotNull Player player, Populator<?> populator) {
            this.content(player, (Populator<T>) populator);
        }

        /**
         * Modify how the elements are populated on the canvas.
         * The populator is called for each element.
         * Additionally, adds the page manipulation buttons.
         *
         * @param player    the player
         * @param populator the populator
         * @return the populator context
         */
        public @NotNull PopulatorContext<T> content(@NotNull Player player, @NotNull Populator<T> populator) {
            this.populator = populator;

            int counter = 0;
            int page = this.canvas.pages.getOrDefault(player.getUniqueId(), 0);
            int elementsPerPage = this.canvas.rows * 9;
            // ig one way of getting the elements per page
            if (this.viewStrategy != null) {
                // it's kinda hacky, but it works
                elementsPerPage = this.viewStrategy.handle(0, this.canvas, this.canvas.hackyButton());
            }
            // max possible page
            int maxPage = (int) Math.ceil((double) this.elements.size() / elementsPerPage) - 1;

            // page manipulation
            if (this.pageForwards != null && this.elements.size() > elementsPerPage && page < maxPage) {
                this.canvas.button(this.pageForwards, (target, clicked, event, canvas) -> {
                    UUID uniqueId = target.getUniqueId();
                    int _page = this.canvas.pages.getOrDefault(uniqueId, 0);
                    this.canvas.pages.put(uniqueId, _page + 1);
                    Menu menu = this.canvas.assosiatedMenu();
                    menu.debug("Page: " + _page);
                    MenuManager manager = menu.manager();
                    if (manager != null) { // should never be null
                        manager.internallyOpen(target, menu);
                    }
                }).handleException(Throwable::printStackTrace);
            }
            if (this.pageBackwards != null && page > 0) {
                this.canvas.button(this.pageBackwards, (target, clicked, event, canvas) -> {
                    UUID uniqueId = target.getUniqueId();
                    int _page = this.canvas.pages.getOrDefault(uniqueId, 0);
                    if (_page > 0) {
                        this.canvas.pages.put(uniqueId, _page - 1);
                    }
                    Menu menu = this.canvas.assosiatedMenu();
                    menu.debug("Page: " + _page);
                    MenuManager manager = menu.manager();
                    if (manager != null) { // should never be null
                        manager.internallyOpen(target, menu);
                    }
                }).handleException(Throwable::printStackTrace);
            }

            // populating elements
            for (int i = 0; i < elementsPerPage; i++) {
                int index = i + (page * elementsPerPage); // the index of the element
                if (index >= this.elements.size()) {
                    break; // no more elements
                }

                // getting the element
                T element = this.elements.get(index);
                Button button = Button.create(counter); // creating the button
                button.populated(true); // mark as populated

                if (this.viewStrategy != null) {
                    // modifying the button based on the view strategy
                    this.viewStrategy.handle(counter, this.canvas, button);
                }

                // populating the element
                populator.populate(element, this.canvas, button);
                counter++;
            }
            return this;
        }

        /**
         * Return to the canvas.
         *
         * @return the canvas
         */
        public @NotNull Canvas end() {
            return this.canvas;
        }

        /**
         * Default view strategies.
         */
        @Accessors(fluent = true, chain = true)
        public enum DefaultViewStrategy {
            /**
             * Fills all first possible slots on the canvas.
             */
            FULL((counter, canvas, button) -> {
                button.slot(counter);
                return canvas.rows * 9;
            }),

            /**
             * Fills all first possible middle slots on the canvas.
             */
            MIDDLE((counter, canvas, button) -> {
                List<Integer> middleSlots = DefaultViewStrategy.middleSlots(canvas.rows());
                if (middleSlots.size() > counter) {
                    button.slot(middleSlots.get(counter));
                }
                return middleSlots.size();
            }),

            /**
             * Fills all slots on the canvas, except the edges on the sides.
             * Note that this strategy DOES fill the upper and lower edges.
             */
            AVOID_SIDE_EDGES((counter, canvas, button) -> {
                List<Integer> middleSlots = DefaultViewStrategy.avoidEdges(canvas.rows());
                if (middleSlots.size() > counter) {
                    button.slot(middleSlots.get(counter));
                }
                return middleSlots.size();
            })

            ;

            @Getter
            private final ViewStrategy viewStrategy;

            DefaultViewStrategy(ViewStrategy viewStrategy) {
                this.viewStrategy = viewStrategy;
            }

            /**
             * Gets all middle slots from the given sized menu.
             * The middle slots are the slots that are not on the edges.
             *
             * @param rows the rows
             * @return the middle slots
             */
            public static List<Integer> middleSlots(int rows) {
                List<Integer> middleSlots = new ArrayList<>();
                if (rows > 2) {
                    int middleRows = rows - 2;
                    for (int i = 0; i < middleRows; i++) {
                        int startSlot = 10 + 9 * i;
                        for (int j = 0; j < 7; j++) {
                            middleSlots.add(startSlot + j);
                        }
                    }
                }
                return middleSlots;
            }

            /**
             * Gets all slots that are not on the edges.
             * @param rows the rows
             * @return the slots
             */
            public static List<Integer> avoidEdges(int rows) {
                List<Integer> middleSlots = new ArrayList<>();
                for (int i = 0; i < rows; i++) {
                    int min = 9 * i + 1;
                    int max = min + 8 - 1;
                    for (int j = min; j <= max; j++) {
                        middleSlots.add(j);
                    }
                }
                return middleSlots;
            }
        }

        /**
         * Populates one element and provides the player with the button.
         *
         * @param <T> the type of the element
         */
        @FunctionalInterface
        public interface Populator<T> {

            /**
             * Populates the element.
             *
             * @param element the element to populate
             * @param canvas  the canvas
             * @param button  the button associated with the element on the canvas
             */
            void populate(@NotNull T element, @NotNull Canvas canvas, @NotNull Button button);
        }

        /**
         * Holds the strategy on how to populate the elements on the canvas.
         * If a menu requires multiple pages, this is the strategy to use.
         * The strategy allows the modification of the area in which the elements are populated.
         */
        @FunctionalInterface
        public interface ViewStrategy {

            /**
             * Allows you to modify the button depending on the given context.
             *
             * @param counter the counter
             * @param canvas  the canvas
             * @param button  the button to modify if needed
             * @return elements per page
             */
            int handle(int counter, @NotNull Canvas canvas, @NotNull Button button);
        }
    }

    /**
     * A button click context.
     */
    @Data
    public static class ClickContext {
        private final @NotNull Canvas canvas;
        private final @Nullable ButtonClick click;
        private @Nullable Consumer<Throwable> throwableConsumer;

        public @NotNull ClickContext handleException(@NotNull Consumer<Throwable> exception) {
            this.throwableConsumer = exception;
            return this;
        }

        public @NotNull Canvas end() {
            return this.canvas;
        }
    }
}

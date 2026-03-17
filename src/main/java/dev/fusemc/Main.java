package dev.fusemc;

import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Context;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Main {

    static void main() {
        try (var ctx = Context.create("js")) {
            var person = ctx.eval("js", """
                    (() => {
                        const person = {
                            name: "Marie",
                            age: 18
                        }
                        return [
                            {
                                name: "Sophia",
                                friend: person
                            },
                            {
                                name: "Elka",
                                friend: person
                            }
                        ]
                    })()
                    """);
            System.out.println(Tau.describe(person).stringify());
        }
    }

    record Person(@NotNull String name, Person @NotNull[] friends) {

        public static final Template<Person> PERSON = Template.recursive(self ->
                Template.record(
                        Template.property("name", Template.STRING, Person::name),
                        Template.optional(
                                Template.property("friends", Template.array(self, Person[]::new), Person::friends),
                                () -> new Person[0]
                        ),
                        Person::new
                )
        );

        @Override
        public @NotNull String toString() {
            return String.format("Person[name=%s, friends=%s]", this.name, Arrays.toString(this.friends));
        }
    }
}

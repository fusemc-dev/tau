package dev.fusemc;

import dev.fusemc.tau.Template;
import org.graalvm.polyglot.Context;

public class Main {

    static void main() {
        try (var ctx = Context.create("js")) {
            var value = ctx.eval("js", "42");
            var template = Template.recursive(t -> t);
            System.out.println(template.lower(value));
        }
    }
}

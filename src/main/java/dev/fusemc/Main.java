package dev.fusemc;

import com.manchickas.optionated.Option;
import dev.fusemc.tau.Tau;
import dev.fusemc.tau.Template;
import dev.fusemc.tau.template.dictionary.record.Record;
import org.graalvm.polyglot.Context;
import org.jetbrains.annotations.NotNull;

public class Main {

    static void main() {
        try (var ctx = Context.create("js")) {
            var value = ctx.eval("js", """
                    (42)
                    """);
            System.out.println(Tau.expect(Shape.TEMPLATE, value));
        }
    }

    interface Shape {

        Template<Shape> TEMPLATE = Template.dispatch(
                Template.STRING.property("type", Shape::type),
                type -> switch (type) {
                    case "circle" -> Option.some(Circle.TEMPLATE);
                    case "rectangle" -> Option.some(Rectangle.TEMPLATE);
                    default -> Option.none();
                }
        );

        @NotNull String type();

        double area();
    }

    record Circle(double radius) implements Shape {

        public static final Record<Circle> TEMPLATE = Template.record(
                Template.DOUBLE.property("radius", Circle::radius),
                Circle::new
        );

        @Override
        public double area() {
            return Math.PI * Math.pow(this.radius, 2);
        }

        @Override
        public @NotNull String type() {
            return "circle";
        }
    }

    record Rectangle(double width, double height) implements Shape {

        public static final Record<Rectangle> TEMPLATE = Template.record(
                Template.DOUBLE.property("width", Rectangle::width),
                Template.DOUBLE.property("height", Rectangle::height),
                Rectangle::new
        );

        @Override
        public double area() {
            return this.width * this.height;
        }

        @Override
        public @NotNull String type() {
            return "rectangle";
        }
    }
}

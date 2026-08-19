package laoqi123.render;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public enum BoxVertexIterator {
    FACE {
        @Override
        public void forEachVertex(Box box, Consumer consumer) {
            int i = 0;
            consumer.accept(i++, box.minX, box.minY, box.minZ);
            consumer.accept(i++, box.maxX, box.minY, box.minZ);
            consumer.accept(i++, box.maxX, box.minY, box.maxZ);
            consumer.accept(i++, box.minX, box.minY, box.maxZ);

            consumer.accept(i++, box.minX, box.maxY, box.minZ);
            consumer.accept(i++, box.minX, box.maxY, box.maxZ);
            consumer.accept(i++, box.maxX, box.maxY, box.maxZ);
            consumer.accept(i++, box.maxX, box.maxY, box.minZ);

            consumer.accept(i++, box.minX, box.minY, box.minZ);
            consumer.accept(i++, box.minX, box.maxY, box.minZ);
            consumer.accept(i++, box.maxX, box.maxY, box.minZ);
            consumer.accept(i++, box.maxX, box.minY, box.minZ);

            consumer.accept(i++, box.maxX, box.minY, box.minZ);
            consumer.accept(i++, box.maxX, box.maxY, box.minZ);
            consumer.accept(i++, box.maxX, box.maxY, box.maxZ);
            consumer.accept(i++, box.maxX, box.minY, box.maxZ);

            consumer.accept(i++, box.minX, box.minY, box.maxZ);
            consumer.accept(i++, box.maxX, box.minY, box.maxZ);
            consumer.accept(i++, box.maxX, box.maxY, box.maxZ);
            consumer.accept(i++, box.minX, box.maxY, box.maxZ);

            consumer.accept(i++, box.minX, box.minY, box.minZ);
            consumer.accept(i++, box.minX, box.minY, box.maxZ);
            consumer.accept(i++, box.minX, box.maxY, box.maxZ);
            consumer.accept(i++, box.minX, box.maxY, box.minZ);
        }

        @Override
        public int sideMask(Direction side) {
            return switch (side) {
                case DOWN -> 0x00000F;
                case UP -> 0x0000F0;
                case NORTH -> 0x000F00;
                case EAST -> 0x00F000;
                case SOUTH -> 0x0F0000;
                case WEST -> 0xF00000;
            };
        }
    },
    OUTLINE {
        @Override
        public void forEachVertex(Box box, Consumer consumer) {
            int i = 0;
            consumer.accept(i++, box.minX, box.minY, box.minZ);
            consumer.accept(i++, box.maxX, box.minY, box.minZ);

            consumer.accept(i++, box.maxX, box.minY, box.minZ);
            consumer.accept(i++, box.maxX, box.minY, box.maxZ);

            consumer.accept(i++, box.maxX, box.minY, box.maxZ);
            consumer.accept(i++, box.minX, box.minY, box.maxZ);

            consumer.accept(i++, box.minX, box.minY, box.maxZ);
            consumer.accept(i++, box.minX, box.minY, box.minZ);

            consumer.accept(i++, box.minX, box.minY, box.minZ);
            consumer.accept(i++, box.minX, box.maxY, box.minZ);

            consumer.accept(i++, box.maxX, box.minY, box.minZ);
            consumer.accept(i++, box.maxX, box.maxY, box.minZ);

            consumer.accept(i++, box.maxX, box.minY, box.maxZ);
            consumer.accept(i++, box.maxX, box.maxY, box.maxZ);

            consumer.accept(i++, box.minX, box.minY, box.maxZ);
            consumer.accept(i++, box.minX, box.maxY, box.maxZ);

            consumer.accept(i++, box.minX, box.maxY, box.minZ);
            consumer.accept(i++, box.maxX, box.maxY, box.minZ);

            consumer.accept(i++, box.maxX, box.maxY, box.minZ);
            consumer.accept(i++, box.maxX, box.maxY, box.maxZ);

            consumer.accept(i++, box.maxX, box.maxY, box.maxZ);
            consumer.accept(i++, box.minX, box.maxY, box.maxZ);

            consumer.accept(i++, box.minX, box.maxY, box.maxZ);
            consumer.accept(i++, box.minX, box.maxY, box.minZ);
        }

        @Override
        public int sideMask(Direction side) {
            return switch (side) {
                case DOWN -> 0b0000_0000_0000_0000_1111_1111;
                case UP -> 0b1111_1111_0000_0000_0000_0000;
                case NORTH -> 0b0000_0011_0000_1111_0000_0011;
                case EAST -> 0b0000_1100_0011_1100_0000_1100;
                case SOUTH -> 0b0011_0000_1111_0000_0011_0000;
                case WEST -> 0b1100_0000_1100_0011_1100_0000;
            };
        }
    };

    public abstract void forEachVertex(Box box, Consumer consumer);

    public abstract int sideMask(Direction side);

    @FunctionalInterface
    public interface Consumer {
        void accept(int index, double x, double y, double z);
    }
}

package app.model;

public class TariffItem {
    private final int id;
    private final String name;

    public TariffItem(int id, String name) {
        this.id = id;
        this.name = name;
    }
    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name; // так в ComboBox будет видимо имя
    }
}

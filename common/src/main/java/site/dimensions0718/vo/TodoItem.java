package site.dimensions0718.vo;

public class TodoItem {
    private Integer id;
    private String text;
    private String status;

    public TodoItem() {
    }

    public TodoItem(Integer id, String text, String status) {
        this.id = id;
        this.text = text;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

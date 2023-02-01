package searchengine.services.dto;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;

import java.util.Objects;


public final class Index {
    private final PageEntity pageEntity;
    private final LemmaEntity lemmaEntity;
    private final float rating;

    public Index(PageEntity pageEntity, LemmaEntity lemmaEntity, float rating) {
        this.pageEntity = pageEntity;
        this.lemmaEntity = lemmaEntity;
        this.rating = rating;
    }

    public PageEntity pageEntity() {
        return pageEntity;
    }

    public LemmaEntity lemmaEntity() {
        return lemmaEntity;
    }

    public float rating() {
        return rating;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Index) obj;
        return Objects.equals(this.pageEntity, that.pageEntity) &&
                Objects.equals(this.lemmaEntity, that.lemmaEntity) &&
                Float.floatToIntBits(this.rating) == Float.floatToIntBits(that.rating);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageEntity, lemmaEntity, rating);
    }

    @Override
    public String toString() {
        return "Index[" +
                "pageEntity=" + pageEntity + ", " +
                "lemmaEntity=" + lemmaEntity + ", " +
                "rating=" + rating + ']';
    }

}

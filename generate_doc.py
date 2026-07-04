# -*- coding: utf-8 -*-
from docx import Document
from docx.shared import Pt, RGBColor, Cm, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import copy

doc = Document()

# ---- helpers ----

def set_rtl(paragraph):
    pPr = paragraph._p.get_or_add_pPr()
    bidi = OxmlElement('w:bidi')
    bidi.set(qn('w:val'), '1')
    pPr.append(bidi)
    jc = OxmlElement('w:jc')
    jc.set(qn('w:val'), 'right')
    pPr.append(jc)

def set_rtl_run(run):
    rPr = run._r.get_or_add_rPr()
    rtl = OxmlElement('w:rtl')
    rPr.append(rtl)
    cs = OxmlElement('w:cs')
    rPr.append(cs)

def add_rtl_heading(doc, text, level=1):
    p = doc.add_heading(text, level=level)
    set_rtl(p)
    for run in p.runs:
        set_rtl_run(run)
        run.font.name = 'Arial'
    return p

def add_rtl_para(doc, text, bold=False, size=11, color=None, space_before=0, space_after=6):
    p = doc.add_paragraph()
    set_rtl(p)
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.space_after = Pt(space_after)
    run = p.add_run(text)
    run.font.name = 'Arial'
    run.font.size = Pt(size)
    run.bold = bold
    set_rtl_run(run)
    if color:
        run.font.color.rgb = RGBColor(*color)
    return p

def add_rtl_bullet(doc, text, indent_level=0):
    p = doc.add_paragraph(style='List Bullet')
    set_rtl(p)
    pPr = p._p.get_or_add_pPr()
    ind = OxmlElement('w:ind')
    ind.set(qn('w:right'), str(360 + indent_level * 360))
    ind.set(qn('w:left'), '0')
    pPr.append(ind)
    run = p.add_run(text)
    run.font.name = 'Arial'
    run.font.size = Pt(10.5)
    set_rtl_run(run)
    return p

def add_rtl_numbered(doc, text):
    p = doc.add_paragraph(style='List Number')
    set_rtl(p)
    run = p.add_run(text)
    run.font.name = 'Arial'
    run.font.size = Pt(10.5)
    set_rtl_run(run)
    return p

def add_separator(doc):
    p = doc.add_paragraph()
    pPr = p._p.get_or_add_pPr()
    pBdr = OxmlElement('w:pBdr')
    bottom = OxmlElement('w:bottom')
    bottom.set(qn('w:val'), 'single')
    bottom.set(qn('w:sz'), '6')
    bottom.set(qn('w:space'), '1')
    bottom.set(qn('w:color'), '4472C4')
    pBdr.append(bottom)
    pPr.append(pBdr)
    return p

# ---- set default font ----
style = doc.styles['Normal']
style.font.name = 'Arial'
style.font.size = Pt(11)

# ============================
# TITLE PAGE
# ============================
doc.add_paragraph()  # top spacing

p_title = doc.add_paragraph()
set_rtl(p_title)
p_title.paragraph_format.space_before = Pt(60)
p_title.paragraph_format.space_after = Pt(10)
run = p_title.add_run('מערכת המלצת מאמרים')
run.font.name = 'Arial'
run.font.size = Pt(28)
run.bold = True
run.font.color.rgb = RGBColor(31, 78, 121)
set_rtl_run(run)

p_sub = doc.add_paragraph()
set_rtl(p_sub)
p_sub.paragraph_format.space_after = Pt(6)
run = p_sub.add_run('Paper Recommender System')
run.font.name = 'Arial'
run.font.size = Pt(16)
run.font.color.rgb = RGBColor(68, 114, 196)
set_rtl_run(run)

p_desc = doc.add_paragraph()
set_rtl(p_desc)
p_desc.paragraph_format.space_after = Pt(4)
run = p_desc.add_run('מסמך תיעוד טכני')
run.font.name = 'Arial'
run.font.size = Pt(13)
run.font.color.rgb = RGBColor(89, 89, 89)
set_rtl_run(run)

add_rtl_para(doc, 'גרסה 1.0  |  מאי 2026', size=10, color=(128, 128, 128))

add_separator(doc)
doc.add_paragraph()

# ============================
# 1. תיאור כללי
# ============================
add_rtl_heading(doc, '1. תיאור כללי', level=1)
add_rtl_para(doc, 'מערכת המלצת מאמרים היא אפליקציית ווב המשלבת מספר אלגוריתמים חכמים לאיתור וסינון מאמרים אקדמיים רלוונטיים. המערכת מציעה המלצות מותאמות אישית בהתבסס על היסטוריית האינטראקציות של המשתמש, תוכן המאמרים ומבנה הציטוטים.')
add_rtl_para(doc, 'המטרה העיקרית היא לסייע לחוקרים ולסטודנטים למצוא מאמרים רלוונטיים ביעילות, תוך שימוש בטכנולוגיות עיבוד שפה טבעית וחיפוש וקטורי.')

doc.add_paragraph()

# ============================
# 2. סטאק טכנולוגי
# ============================
add_rtl_heading(doc, '2. סטאק טכנולוגי', level=1)

add_rtl_heading(doc, 'שפות וסביבה', level=2)
add_rtl_bullet(doc, 'Java 17 — שפת הפיתוח הראשית')
add_rtl_bullet(doc, 'Spring Boot 3.2.5 — מסגרת אפליקציית הווב')
add_rtl_bullet(doc, 'Maven — כלי לבניית הפרויקט')
add_rtl_bullet(doc, 'C (JNI) — ספריות נייטיב לאלגוריתמים ביצועיים')

add_rtl_heading(doc, 'תשתיות', level=2)
add_rtl_bullet(doc, 'PostgreSQL (Neon Cloud) — מסד נתונים ראשי')
add_rtl_bullet(doc, 'Flyway — ניהול מיגרציות של בסיס הנתונים')
add_rtl_bullet(doc, 'HikariCP — מאגר חיבורים (pool size=5)')

add_rtl_heading(doc, 'תלויות עיקריות (pom.xml)', level=2)
add_rtl_bullet(doc, 'spring-boot-starter-web')
add_rtl_bullet(doc, 'spring-boot-starter-data-jpa')
add_rtl_bullet(doc, 'postgresql driver')
add_rtl_bullet(doc, 'flyway-core')
add_rtl_bullet(doc, 'jackson-databind')
add_rtl_bullet(doc, 'junit-jupiter')

doc.add_paragraph()

# ============================
# 3. ארכיטקטורה
# ============================
add_rtl_heading(doc, '3. ארכיטקטורה', level=1)
add_rtl_para(doc, 'הפרויקט מאורגן בחבילות (packages) ברורות לפי אחריות:')

add_rtl_heading(doc, 'מבנה החבילות', level=2)

items = [
    ('model/', 'מודלי הנתונים: Paper, User, Interaction'),
    ('algorithm/', 'אלגוריתמים: HNSW, PageRank, ALS, BPR, LDA, BM25, Fusion/MMR'),
    ('api/', 'לקוחות API חיצוניים: Semantic Scholar, OpenAlex, ArXiv'),
    ('db/', 'שכבת בסיס הנתונים: Entities, Repositories, Converters'),
    ('jni/', 'גשר JNI לספריות נייטיב: BM25Native, HnswNative, NativeLoader'),
    ('service/', 'לוגיקת עסקים: PaperService, IndexService, RecommendationService וכו\''),
    ('web/', 'בקרים (Controllers) ו-DTOs לממשק HTTP'),
    ('config/', 'הגדרות: DataLoader, IndexWarmUpRunner, ApiClientConfig'),
]
for pkg, desc in items:
    p = doc.add_paragraph()
    set_rtl(p)
    pPr = p._p.get_or_add_pPr()
    ind = OxmlElement('w:ind')
    ind.set(qn('w:right'), '360')
    pPr.append(ind)
    r1 = p.add_run(pkg + ' ')
    r1.font.name = 'Courier New'
    r1.font.size = Pt(10)
    r1.bold = True
    r1.font.color.rgb = RGBColor(31, 78, 121)
    set_rtl_run(r1)
    r2 = p.add_run('— ' + desc)
    r2.font.name = 'Arial'
    r2.font.size = Pt(10.5)
    set_rtl_run(r2)

doc.add_paragraph()

# ============================
# 4. מודלי נתונים
# ============================
add_rtl_heading(doc, '4. מודלי נתונים', level=1)

add_rtl_heading(doc, 'Paper (מאמר)', level=2)
add_rtl_bullet(doc, 's2Id, arxivId, doi — מזהים חיצוניים')
add_rtl_bullet(doc, 'title, abstract, year — מטא-נתונים')
add_rtl_bullet(doc, 'topics — רשימת נושאים (topics)')
add_rtl_bullet(doc, 'float[768] embedding — וקטור הטמעה סמנטי')
add_rtl_bullet(doc, 'citedPaperIds — מזהי מאמרים מצוטטים')

add_rtl_heading(doc, 'User (משתמש)', level=2)
add_rtl_bullet(doc, 'UUID PK — מזהה ייחודי')
add_rtl_bullet(doc, 'name — שם המשתמש')

add_rtl_heading(doc, 'Interaction (אינטראקציה)', level=2)
add_rtl_bullet(doc, 'userId / paperId — UUID FKs')
add_rtl_bullet(doc, 'ActionType (Enum): VIEW=0.2, CLICK=0.5, SAVE=1.0, DOWNLOAD=1.5, CITE=3.0')
add_rtl_bullet(doc, 'createdAt — חותמת זמן')

doc.add_paragraph()

# ============================
# 5. אלגוריתמים
# ============================
add_rtl_heading(doc, '5. אלגוריתמים', level=1)

algos = [
    ('HNSW', 'Hierarchical Navigable Small World',
     'חיפוש וקטורי בקרבה (ANN) על בסיס embeddings של מאמרים. ספריית נייטיב (C/JNI) לביצועים אופטימליים.'),
    ('BM25', 'Best Match 25',
     'חיפוש טקסט מלא (full-text search) על בסיס מילים. ממומש בספריית נייטיב (C/JNI).'),
    ('PageRank', 'PageRank',
     'דירוג מאמרים לפי מבנה גרף הציטוטים — מאמרים מצוטטים יותר מקבלים ציון גבוה יותר.'),
    ('ALS', 'Alternating Least Squares (Collaborative Filtering)',
     'סינון שיתופי מבוסס מטריצת אינטראקציות משתמש–מאמר.'),
    ('BPR', 'Bayesian Personalized Ranking',
     'למידה לדירוג מותאם אישית. ממומש בספריית נייטיב (C/JNI).'),
    ('LDA', 'Latent Dirichlet Allocation',
     'מודל נושאים (topic model) לגילוי נושאים סמויים בטקסט המאמרים.'),
    ('Fusion Reranker', 'Reciprocal Rank Fusion',
     'מיזוג תוצאות מאלגוריתמים שונים לרשימה אחת מאוחדת.'),
    ('MMR', 'Maximal Marginal Relevance',
     'ייחוד תוצאות — הפחתת כפילויות תוך שמירה על רלוונטיות (λ=0.7).'),
]

for name, full_name, desc in algos:
    p = doc.add_paragraph()
    set_rtl(p)
    p.paragraph_format.space_before = Pt(4)
    r1 = p.add_run(name + ' ')
    r1.font.name = 'Arial'
    r1.font.size = Pt(11)
    r1.bold = True
    r1.font.color.rgb = RGBColor(31, 78, 121)
    set_rtl_run(r1)
    r2 = p.add_run('(' + full_name + ') — ')
    r2.font.name = 'Arial'
    r2.font.size = Pt(11)
    r2.font.color.rgb = RGBColor(89, 89, 89)
    set_rtl_run(r2)
    r3 = p.add_run(desc)
    r3.font.name = 'Arial'
    r3.font.size = Pt(11)
    set_rtl_run(r3)

doc.add_paragraph()
add_rtl_heading(doc, 'פרופיל משתמש', level=2)
add_rtl_para(doc, 'וקטור הפרופיל מחושב בזמן אמת (ללא אחסון קבוע) כממוצע משוקלל של וקטורי ה-embedding של המאמרים שהמשתמש אינטרקט איתם, עם דעיכה אקספוננציאלית לפי זמן:')
add_rtl_para(doc, 'w = action_weight × exp(−λ × ימים_שחלפו)  |  חצי-חיים: 30 ימים', size=10, color=(68, 114, 196))

doc.add_paragraph()

# ============================
# 6. API Endpoints
# ============================
add_rtl_heading(doc, '6. נקודות קצה (API Endpoints)', level=1)

add_rtl_heading(doc, 'המלצות', level=2)
add_rtl_bullet(doc, 'GET /api/recommend?userId=&q=&k= — קבלת המלצות מותאמות אישית')

add_rtl_heading(doc, 'עיבוד נתונים (Ingest)', level=2)
add_rtl_bullet(doc, 'POST /api/ingest/s2?query=&limit=20 — חיפוש ב-Semantic Scholar ושמירה')
add_rtl_bullet(doc, 'POST /api/ingest/s2/{paperId}?maxRefs=50 — עיבוד עמוק: מאמר + הפניות + גרף ציטוטים')
add_rtl_bullet(doc, 'POST /api/ingest/arxiv?query=&maxResults=20 — חיפוש ב-ArXiv')

add_rtl_heading(doc, 'ממשק אדמין', level=2)
add_rtl_bullet(doc, 'POST /api/admin/reindex — בנייה מחדש של HNSW + BM25 + BPR + PageRank')
add_rtl_bullet(doc, 'POST /api/admin/deep-ingest-all?maxRefs=20&delayMs=400 — עיבוד עמוק לכל מאמרי S2')
add_rtl_bullet(doc, 'GET /api/admin/stats — סטטיסטיקות: papersInDb, papersInIndex, bm25Docs, bprTrained')
add_rtl_bullet(doc, 'GET /api/admin/search-debug?q=... — דיבאג חיפוש BM25')

add_rtl_heading(doc, 'CRUD בסיסי', level=2)
add_rtl_bullet(doc, '/api/papers — ניהול מאמרים')
add_rtl_bullet(doc, '/api/users — ניהול משתמשים')
add_rtl_bullet(doc, '/api/interactions — ניהול אינטראקציות')

doc.add_paragraph()

# ============================
# 7. מסד נתונים
# ============================
add_rtl_heading(doc, '7. מסד נתונים', level=1)
add_rtl_para(doc, 'מסד הנתונים: Neon (Serverless PostgreSQL, Cloud — eu-central-1, AWS).')

add_rtl_heading(doc, 'טבלאות', level=2)
add_rtl_bullet(doc, 'papers — מאמרים, embedding כ-bytea (FloatArrayConverter, little-endian float32)')
add_rtl_bullet(doc, 'paper_topics — נושאי מאמרים (@ElementCollection)')
add_rtl_bullet(doc, 'paper_citations — גרף ציטוטים')
add_rtl_bullet(doc, 'users — משתמשים')
add_rtl_bullet(doc, 'interactions — אינטראקציות משתמש–מאמר')

add_rtl_heading(doc, 'הגדרות חיבור', level=2)
add_rtl_bullet(doc, 'HikariCP pool size = 5 (Neon PgBouncer transaction mode)')
add_rtl_bullet(doc, 'Flyway — מיגרציה V1__init.sql')
add_rtl_bullet(doc, 'Port: 8080')

doc.add_paragraph()

# ============================
# 8. API חיצוניים
# ============================
add_rtl_heading(doc, '8. מקורות נתונים חיצוניים', level=1)

add_rtl_bullet(doc, 'Semantic Scholar API — מקור ראשי: מאמרים, הפניות, embeddings, נושאי מחקר (s2FieldsOfStudy)')
add_rtl_bullet(doc, 'ArXiv API — מקור משני עבור מאמרים פרה-פרינט')
add_rtl_bullet(doc, 'OpenAlex API — מקור נוסף (ממומש, אופציונלי)')
add_rtl_para(doc, 'הערה: ה-S2 API ללא מפתח מוגבל לקצב בקשות. מומלץ להוסיף מפתח API ב-application.properties.', size=10, color=(128, 128, 128))

doc.add_paragraph()

# ============================
# 9. ספריות נייטיב
# ============================
add_rtl_heading(doc, '9. ספריות נייטיב (JNI)', level=1)
add_rtl_para(doc, 'ארבע ספריות C נבנות עם CMake ונטענות בזמן ריצה:')
add_rtl_bullet(doc, 'hnsw — חיפוש וקטורי ANN')
add_rtl_bullet(doc, 'bm25 — חיפוש טקסט מלא')
add_rtl_bullet(doc, 'bpr — Bayesian Personalized Ranking')
add_rtl_bullet(doc, 'pagerank — חישוב PageRank על גרף ציטוטים')
add_rtl_para(doc, 'ה-DLLs מועתקים ל-src/main/resources/native/ לאחר הבנייה. NativeLoader טוען אותם מה-classpath בזמן אתחול האפליקציה.')

doc.add_paragraph()

# ============================
# 10. ממשק משתמש
# ============================
add_rtl_heading(doc, '10. ממשק משתמש (Frontend)', level=1)
add_rtl_para(doc, 'ממשק ווב סטטי המוגש ישירות על ידי Spring Boot:')
add_rtl_bullet(doc, 'index.html — ממשק ראשי: חיפוש, כרטיסי מאמרים, המלצות')
add_rtl_bullet(doc, 'admin.html — קונסול ניהול: reindex, deep-ingest-all, stats, search-debug')

add_rtl_heading(doc, 'מצב משתמש ב-localStorage', level=2)
add_rtl_bullet(doc, 'savedIds_<userId> — מאמרים שמורים (per-user)')
add_rtl_bullet(doc, 'citedIds_<userId> — רשימת ההפניות האישית של המשתמש')
add_rtl_para(doc, 'כפתור "Add to refs" / "In my refs" — רשימת ההפניות האישית של המשתמש (לא ציטוטים אקדמיים).', size=10, color=(128, 128, 128))

doc.add_paragraph()

# ============================
# 11. הפעלה ואתחול
# ============================
add_rtl_heading(doc, '11. הפעלה ואתחול', level=1)

add_rtl_heading(doc, 'הפעלת האפליקציה', level=2)
add_rtl_bullet(doc, 'לחיצה על כפתור Run ב-IntelliJ IDEA')
add_rtl_bullet(doc, 'האפליקציה עולה על פורט 8080')
add_rtl_bullet(doc, 'DataLoader זורע ~40 מאמרים מ-Semantic Scholar בהפעלה הראשונה (מדלג אם כבר קיימים ≥10 מאמרים)')

add_rtl_heading(doc, 'בנייה ספריות נייטיב', level=2)
add_rtl_bullet(doc, 'cmake -S native/ -B native/build -DCMAKE_BUILD_TYPE=Release')
add_rtl_bullet(doc, 'cmake --build native/build')
add_rtl_bullet(doc, 'ה-CMakeLists.txt מעתיק את ה-DLLs אוטומטית ל-resources/native/')

add_rtl_heading(doc, 'הפעלה ראשונית — שלבים', level=2)
add_rtl_numbered(doc, 'הרצת האפליקציה — DataLoader זורע מאמרים ומבצע reindex אוטומטי')
add_rtl_numbered(doc, 'POST /api/admin/deep-ingest-all?maxRefs=20&delayMs=400 — לאיכלוס ציטוטים ונושאים')
add_rtl_numbered(doc, 'ממשק המשתמש זמין בכתובת: http://localhost:8080')

doc.add_paragraph()

# ============================
# 12. קבצים חשובים
# ============================
add_rtl_heading(doc, '12. קבצים חשובים', level=1)
files = [
    ('src/main/resources/application.properties', 'חיבור Neon DB, Flyway, פורט'),
    ('src/main/resources/db/migration/V1__init.sql', 'סכמת בסיס הנתונים'),
    ('src/main/resources/static/index.html', 'ממשק משתמש ראשי'),
    ('src/main/resources/static/admin.html', 'קונסול ניהול'),
    ('native/CMakeLists.txt', 'הגדרות בנייה של ספריות C'),
    ('requests.http', 'קבצי HTTP לבדיקות API ב-IntelliJ'),
]
for path, desc in files:
    p = doc.add_paragraph()
    set_rtl(p)
    pPr = p._p.get_or_add_pPr()
    ind = OxmlElement('w:ind')
    ind.set(qn('w:right'), '360')
    pPr.append(ind)
    r1 = p.add_run(path + ' ')
    r1.font.name = 'Courier New'
    r1.font.size = Pt(9.5)
    r1.bold = True
    r1.font.color.rgb = RGBColor(31, 78, 121)
    set_rtl_run(r1)
    r2 = p.add_run('— ' + desc)
    r2.font.name = 'Arial'
    r2.font.size = Pt(10.5)
    set_rtl_run(r2)

doc.add_paragraph()

# ============================
# 13. מה נותר
# ============================
add_rtl_heading(doc, '13. מה נותר לעשות', level=1)
add_rtl_bullet(doc, 'הוספת S2 API key ב-application.properties לקצב בקשות גבוה יותר (כרגע 429 על ingest מרובה)')
add_rtl_bullet(doc, 'Redis — נדחה לשלב עתידי')
add_rtl_bullet(doc, 'TF-IDF — הוחרג על ידי המשתמש')

doc.add_paragraph()
add_separator(doc)
add_rtl_para(doc, 'מסמך זה נוצר אוטומטית — מאי 2026', size=9, color=(128, 128, 128))

# Save
out_path = r'C:\Users\ikomostars\IdeaProjects\PaperRecommender\PaperRecommender_Documentation_HE.docx'
doc.save(out_path)
print('Saved:', out_path)

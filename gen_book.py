# -*- coding: utf-8 -*-
"""Generate full K13 project book (sefer proekt) in Hebrew for PaperRecommender."""
from docx import Document
from docx.shared import Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

doc = Document()

# ============================================================
# HELPERS
# ============================================================
def set_rtl(p):
    pPr = p._p.get_or_add_pPr()
    b = OxmlElement('w:bidi'); b.set(qn('w:val'),'1'); pPr.append(b)
    j = OxmlElement('w:jc'); j.set(qn('w:val'),'right'); pPr.append(j)

def rtl_run(run):
    rPr = run._r.get_or_add_rPr()
    rPr.append(OxmlElement('w:rtl')); rPr.append(OxmlElement('w:cs'))

def h(level, text):
    p = doc.add_heading(text, level=level); set_rtl(p)
    for r in p.runs: rtl_run(r); r.font.name='Arial'
    return p

def para(text, bold=False, sz=12, color=None, align='right', space_after=6, space_before=0):
    p = doc.add_paragraph(); set_rtl(p)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.space_before = Pt(space_before)
    r = p.add_run(text); r.font.name='Arial'; r.font.size=Pt(sz); r.bold=bold; rtl_run(r)
    if color: r.font.color.rgb=RGBColor(*color)
    return p

def bullet(text):
    p = doc.add_paragraph(style='List Bullet'); set_rtl(p)
    r = p.add_run(text); r.font.name='Arial'; r.font.size=Pt(11); rtl_run(r)

def numbered(text):
    p = doc.add_paragraph(style='List Number'); set_rtl(p)
    r = p.add_run(text); r.font.name='Arial'; r.font.size=Pt(11); rtl_run(r)

def code_para(text):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(2)
    r = p.add_run(text); r.font.name='Courier New'; r.font.size=Pt(9)
    return p

def add_table(headers, rows):
    t = doc.add_table(rows=1+len(rows), cols=len(headers))
    t.style = 'Light Grid Accent 1'
    for i,hdr in enumerate(headers):
        c = t.rows[0].cells[i]; c.text = hdr
        for p in c.paragraphs: set_rtl(p)
        for p in c.paragraphs:
            for r in p.runs: r.bold=True; r.font.name='Arial'; r.font.size=Pt(10); rtl_run(r)
    for ri, row in enumerate(rows):
        for ci, val in enumerate(row):
            c = t.rows[ri+1].cells[ci]; c.text = val
            for p in c.paragraphs: set_rtl(p)
            for p in c.paragraphs:
                for r in p.runs: r.font.name='Arial'; r.font.size=Pt(10); rtl_run(r)
    return t

def page_break():
    doc.add_page_break()

def separator():
    p = doc.add_paragraph()
    pPr = p._p.get_or_add_pPr()
    pBdr = OxmlElement('w:pBdr')
    bot = OxmlElement('w:bottom')
    bot.set(qn('w:val'),'single'); bot.set(qn('w:sz'),'6')
    bot.set(qn('w:space'),'1'); bot.set(qn('w:color'),'4472C4')
    pBdr.append(bot); pPr.append(pBdr)

# ── default font ──
style = doc.styles['Normal']; style.font.name='Arial'; style.font.size=Pt(12)

# ── Header / Footer ──
section = doc.sections[0]
header = section.header
hp = header.paragraphs[0]
hp.text = "PaperRecommender  |  מערכת המלצת מאמרים"
hp.style = doc.styles['Header']
for r in hp.runs: r.font.name='Arial'; r.font.size=Pt(9)

footer = section.footer
fp = footer.paragraphs[0]
fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = fp.add_run()
fld = OxmlElement('w:fldSimple')
fld.set(qn('w:instr'), 'PAGE')
r._r.append(fld)

# ============================================================
# 1. COVER PAGE
# ============================================================
doc.add_paragraph()
doc.add_paragraph()
para('מערכת המלצת מאמרים', bold=True, sz=28, color=(31,78,121), space_before=60)
para('Paper Recommender System', sz=16, color=(68,114,196))
separator()
para('ספר פרויקט – י״ג הנדסת תוכנה', bold=True, sz=14, space_before=20)
doc.add_paragraph()
para('שם הסטודנט: ______________________', sz=12)
para('ת.ז. הסטודנט: ______________________', sz=12)
para('שם המנחה: ______________________', sz=12)
para('שם מכללה: ______________________', sz=12)
para('סמל מוסד: ______________________', sz=12)
para('תאריך הגשה: מאי 2026', sz=12)

page_break()

# ============================================================
# 2. TABLE OF CONTENTS
# ============================================================
h(1, 'תוכן עניינים')
para('[יש לייצר תוכן עניינים אוטומטי ב-Word באמצעות References > Table of Contents]', sz=11, color=(128,128,128))

page_break()

# ============================================================
# 3. ABSTRACT
# ============================================================
h(1, '1. תקציר')
para('פרויקט זה מציג מערכת המלצת מאמרים אקדמיים המשלבת מספר אלגוריתמים מתקדמים לאיתור וסינון מאמרים רלוונטיים. המערכת משלבת ארבעה אותות עיקריים: (1) חיפוש וקטורי עם HNSW על בסיס וקטורי הטמעה SPECTER2, (2) סינון שיתופי עם BPR, (3) חיפוש טקסט מלא עם BM25, ו-(4) דירוג מבוסס גרף עם PageRank.')
para('התוצאות ממוזגות באמצעות Reciprocal Rank Fusion ומגוונות עם Maximal Marginal Relevance להבטחת מגוון.')
para('האלגוריתמים הביצועיים (HNSW, BPR, PageRank) ממומשים מאפס בשפת C ומחוברים דרך JNI, בלי שימוש בספריות מוכנות. שכבת הנתונים מבוססת על PostgreSQL (Neon Cloud) עם Spring Boot 3.2.5 ו-Flyway.')
para('המערכת כוללת ממשק ווב, קונסול ניהול, ומנגנון REST API לאינטגרציה עם Semantic Scholar, ArXiv ו-OpenAlex.')

page_break()

# ============================================================
# 4. PROJECT PROPOSAL (placeholder)
# ============================================================
h(1, '2. הצעת פרויקט מאושרת')
para('[יש לצרף כאן את הצעת הפרויקט המאושרת על ידי משרד החינוך]', sz=11, color=(128,128,128))

page_break()

# ============================================================
# 5. DECLARATION
# ============================================================
h(1, '3. תצהיר')
para('אני החתום/ה מטה מצהיר/ה כי עבודה זו בוצעה על ידי בלבד, ללא העתקות או שימוש בקוד של אחרים באופן שאינו מותר וללא שימוש בבינה מלאכותית (AI) לכתיבת הקוד או חלקים מהותיים ממנו.')
doc.add_paragraph()
para('שם: ______________________       תאריך: ______________________       חתימה: ______________________', sz=11)

page_break()

# ============================================================
# 6. TERMINOLOGY
# ============================================================
h(1, '4. מושגים')
terms = [
    ('Embedding (וקטור הטמעה)', 'ייצוג מספרי של טקסט במרחב וקטורי רב-ממדי (768 ממדים). מאמרים דומים מבחינה סמנטית יהיו קרובים במרחב זה.'),
    ('SPECTER2', 'מודל הטמעה למאמרים אקדמיים מבית Semantic Scholar, מייצר וקטורים באורך 768 ממדים.'),
    ('ANN (Approximate Nearest Neighbor)', 'חיפוש שכנים קרובים במרחב וקטורי בצורה מקורבת (לא מדויקת) — מאפשר חיפוש בזמן O(log N) במקום O(N).'),
    ('HNSW (Hierarchical Navigable Small World)', 'מבנה נתונים גרפי רב-שכבתי לחיפוש ANN. מורכב משכבות היררכיות כדוגמת skip list.'),
    ('BM25 (Okapi BM25)', 'אלגוריתם לדירוג טקסט מלא המשלב TF-IDF עם נורמליזציה לאורך מסמך ורוויית TF.'),
    ('BPR (Bayesian Personalized Ranking)', 'אלגוריתם לסינון שיתופי הלומד לדרג זוגות פריטים מתוך משוב מרומז (implicit feedback).'),
    ('PageRank', 'אלגוריתם דירוג צמתים בגרף מכוון — צומת שמקבל הרבה קישורים נחשב חשוב יותר.'),
    ('RRF (Reciprocal Rank Fusion)', 'שיטת מיזוג מספר רשימות מדורגות לרשימה אחת אופטימלית, ללא תלות בסקאלות הציונים.'),
    ('MMR (Maximal Marginal Relevance)', 'אלגוריתם לגיוון תוצאות — מפחית כפילויות תוך שמירה על רלוונטיות.'),
    ('Cosine Similarity', 'מדד דמיון בין שני וקטורים: cos(θ) = (A·B) / (||A||·||B||). טווח [−1, 1].'),
    ('JNI (Java Native Interface)', 'ממשק המאפשר לקוד Java לקרוא לפונקציות בשפת C לביצועים אופטימליים.'),
    ('Implicit Feedback', 'משוב עקיף שנובע מהתנהגות המשתמש (צפייה, לחיצה, שמירה) ולא מדירוג מפורש.'),
]
add_table(['הגדרה','מושג'], [[t,n] for n,t in terms])

page_break()

# ============================================================
# 7. TOPIC DESCRIPTION
# ============================================================
h(1, '5. תיאור הנושא')
para('מערכות המלצה (Recommender Systems) הן מערכות תוכנה המסננות פריטים רלוונטיים למשתמש מתוך מאגר גדול של פריטים. בעולם האקדמי, כמות המאמרים המתפרסמים מדי שנה עולה על 2.5 מיליון, וחוקר או סטודנט אינו יכול לקרוא את כולם.')
para('הבעיה המרכזית: כיצד לבחור K מאמרים הרלוונטיים ביותר למשתמש מסוים, כאשר הרלוונטיות תלויה במספר אותות — תוכן המאמר, היסטוריית המשתמש, ומבנה הציטוטים. זוהי בעיה של הכרעת מצבים — אין קריטריון יחיד לפתרון, ונדרש שילוב של מספר אותות.')

page_break()

# ============================================================
# 8. THEORETICAL BACKGROUND
# ============================================================
h(1, '6. רקע תיאורטי')
para('בתחום מערכות המלצה קיימות מספר גישות עיקריות:')
h(2, 'סינון מבוסס תוכן (Content-Based Filtering)')
para('גישה המשתמשת בתכונות הפריטים עצמם — טקסט, מונחים, וקטורי הטמעה. המערכת משתמשת בוקטורי SPECTER2 ובאלגוריתם HNSW למציאת מאמרים דומים מבחינה סמנטית.')
h(2, 'סינון שיתופי (Collaborative Filtering)')
para('גישה המתבססת על דפוסי התנהגות של משתמשים דומים. BPR (Rendle et al., 2009) לומד דירוג זוגי (pairwise ranking) מתוך משוב מרומז (implicit feedback) באמצעות SGD.')
h(2, 'דירוג מבוסס גרף (Graph-Based Ranking)')
para('PageRank (Brin & Page, 1998) מדרג צמתים לפי מבנה קישורים — מאמר שמצוטט על ידי מאמרים חשובים מקבל ציון גבוה יותר.')
h(2, 'חיפוש טקסט (Text Retrieval)')
para('BM25 (Robertson & Zaragoza, 2009) הוא אלגוריתם סטנדרטי לחיפוש טקסט, המשלב IDF עם נורמליזציה לאורך המסמך ורוויית תדירות.')
h(2, 'מיזוג וגיוון')
para('RRF (Cormack et al., 2009) ממזג תוצאות ממספר רנקרים ללא צורך לנרמל ציונים. MMR (Carbonell & Goldstein, 1998) מבטיח גיוון על ידי הפחתת דמיון בין תוצאות שנבחרו.')

page_break()

# ============================================================
# 9. ALGORITHMIC PROBLEM DESCRIPTION
# ============================================================
h(1, '7. תיאור הבעיה האלגוריתמית')
para('הבעיה המרכזית: בהינתן מאגר של N מאמרים, משתמש עם היסטוריית אינטראקציות, ושאילתת טקסט אופציונלית — יש למצוא את K המאמרים הרלוונטיים ביותר.')
para('זוהי בעיה של הכרעת מצבים מכיוון ש:', bold=True)
bullet('אין קריטריון יחיד לרלוונטיות — הרלוונטיות תלויה בתוכן, בהתנהגות המשתמש, במבנה הציטוטים, ובטקסט החופשי')
bullet('כל אות ומיזוג נותן תוצאות שונות — איך לשקלל את האותות הללו? איזה עדיפות? אין תשובה אופטימלית מובטחת')
bullet('חיפוש k-NN מדויק במרחב 768 ממדים הוא O(N) — לא יעיל למאגרים גדולים')
bullet('פרופיל המשתמש משתנה בזמן — דרושה דעיכה אקספוננציאלית')
para('לכן הפתרון דורש שילוב של מספר אותות עם היוריסטיקות, כאשר לכל אות יש חוזקות וחסרונות. אין פתרון מושלם — ההכרעה נעשית בצורה היוריסטית מבוססת מחקר.')

page_break()

# ============================================================
# 10. ALGORITHM SURVEY
# ============================================================
h(1, '8. סקירת אלגוריתמים בתחום הבעיה')
para('להלן סקירה של האלגוריתמים העיקריים בתחום מערכות המלצה ואיך הם הושוו בפרויקט:')
survey = [
    ('אלגוריתם','סיבוכיות','חיסרונות','שימוש בפרויקט'),
    ('HNSW','חיפוש O(log N), מדרגי (מקורב)','דורש זיכרון נוסף','כן — נייטיב C'),
    ('k-d Tree','מדויק בממדים נמוכים','מתפרק ל-O(N) בממדים גבוהים','לא — 768 ממדים'),
    ('LSH','מהיר, פשוט','דיוק נמוך מ-HNSW','לא'),
    ('BM25','אינדקס הפוך, יעיל','לא תופס סמנטיקה','כן — Java'),
    ('TF-IDF','פשוט','ללא נורמליזציה לאורך','לא — BM25 עדיף'),
    ('BPR','לומד דירוג מ-implicit','דורש הרבה נתונים','כן — נייטיב C'),
    ('ALS','מקבילי ל-explicit','לא מתאים ל-implicit','לא'),
    ('PageRank','מדרג אוטוריטה','לא אישי לבדו','כן — נייטיב C'),
    ('RRF','ממזג ללא נורמליזציה','משקל שווה','כן — Java'),
    ('MMR','מגוון עם פרמטר λ','ריבועי O(k²)','כן — Java'),
]
add_table(survey[0], survey[1:])

page_break()

# ============================================================
# 11. CHOSEN STRATEGY + JUSTIFICATION (3-4 pages)
# ============================================================
h(1, '9. אסטרטגיה/אלגוריתם נבחר/ת לפתרון — תיאור ונימוק')
para('האסטרטגיה הנבחרת היא צינור היברידי (Hybrid Pipeline) המשלב ארבעה אותות בצינור צב הערכים:', bold=True)
doc.add_paragraph()
h(2, '9.1 HNSW — חיפוש וקטורי (Content-Based)')
para('נימוק: HNSW (Malkov & Yashunin, 2020) נבחר משום שהוא מציע חיפוש בזמן O(log N) לעומת O(N) בחיפוש ברוט-פורס, והוא עמיד בממדים גבוהים (768) בניגוד ל-k-d Tree.')
para('מבנה: גרף רב-שכבתי (Navigable Small World) עם מספר שכבות היררכיות — השכבה העליונה מכילה "skip links" לקפיצות מהירות. בשלב החיפוש, יורדים מהשכבה העליונה לתחתונה, ובשכבה 0 מתבצע חיפוש beam (ef) לדיוק גבוה.')
para('פרמטרים: M=16, ef_construction=200, ef_search=50, cosine distance.', sz=11)
para('מדד מרחק: cosine distance = 1 − cos(θ). מימוש: נייטיב C עם heap ו-xorshift64 RNG.', sz=11)
doc.add_paragraph()
h(2, '9.2 BPR — סינון שיתופי (Collaborative Filtering)')
para('נימוק: BPR (Rendle et al., 2009) מתאים במיוחד למערכת עם משוב מרומז (implicit feedback) — המשתמש לא נותן דירוג מפורש, אלא אנו לומדים מהתנהגותו. ALS לא מתאים כי הוא מיועד ל-explicit ratings.')
para('מודל: מטריצת פירוק (factorization) — לכל משתמש u ופריט i יש וקטור באורך 64 (factors). ציון התחזית: score(u,i) = dot(U_u, V_i).')
para('אימון: SGD עם דגימת זוגות — לכל זוג (u, i_pos, i_neg): ממקסמים log σ(x_uij), כאשר x_uij = dot(U_u, V_pos) − dot(U_u, V_neg).')
para('פרמטרים: factors=64, epochs=100, lr=0.01, λ=0.001 (L2 reg), seed=42.', sz=11)
doc.add_paragraph()
h(2, '9.3 BM25 — חיפוש טקסט (Text Search)')
para('נימוק: BM25 מספק חיפוש טקסט יעיל כאשר המשתמש מקליד שאילתת חיפוש (query). הוא עדיף על TF-IDF בכך שהוא מנרמל את תדירות המונח (TF saturation) ואת אורך המסמך.')
para('נוסחה:', bold=True)
code_para('BM25(d, Q) = Σ_{t ∈ Q} IDF(t) · tf(t,d)·(k1+1) / (tf(t,d) + k1·(1 − b + b·|d|/avgdl))')
code_para('IDF(t) = ln((N − df(t) + 0.5) / (df(t) + 0.5) + 1)')
para('פרמטרים: k1=1.2, b=0.75.', sz=11)
doc.add_paragraph()
h(2, '9.4 PageRank — דירוג גרף ציטוטים')
para('נימוק: גרף הציטוטים נותן אות נוסף — מאמר שמצוטט על ידי מאמרים חשובים אמור להיות משמעותי יותר.')
para('נוסחה:', bold=True)
code_para('PR(p) = (1−d)/N + d · Σ_{q∈B(p)} PR(q)/L(q)')
para('כאשר d=damping (0.85), B(p)=מצטטים, L(q)=דרגת יציאה. צמתים תלויים מחולקים באחידות. 20 איטרציות.', sz=11)
doc.add_paragraph()
h(2, '9.5 RRF + MMR — מיזוג וגיוון')
para('RRF: score(d) = Σ_r 1/(k + rank_r(d)), k=60. ממזג 4 רשימות (HNSW, BPR, BM25, PageRank) לאחת.', bold=True)
para('MMR: argmax_{d ∈ R\\S} [λ·rel(d) − (1−λ)·max_{s ∈ S} sim(d,s)], λ=0.7.', bold=True)
para('נימוק ל-RRF: אינו דורש נורמליזציה של ציונים — כל אות נותן רק דירוג. נימוק ל-MMR: מונע מהמשתמש לראות רשימה שכולה מאמרים דומים מדי.')

page_break()
h(2, '9.6 פרופיל משתמש — דעיכה אקספוננציאלית')
para('וקטור הפרופיל של המשתמש מחושב בזמן אמת כממוצע משוקלל של וקטורי המאמרים שהמשתמש אינטראקט איתם:')
code_para('w = action_weight × exp(−λ × days_since)')
code_para('profile = Σ w_i · emb_i / Σ w_i')
para('חצי-חיים: 30 ימים. משקלות: VIEW=0.2, CLICK=0.5, SAVE=1.0, DOWNLOAD=1.5, CITE=3.0.', sz=11)
para('הדעיכה מבטיחה שהפרופיל משקף את האינטרסים הנוכחיים של המשתמש ולא את הישנים.')

page_break()

# ============================================================
# 12. ARCHITECTURE — Top-Down Level Design
# ============================================================
h(1, '10. ארכיטקטורה — Top-Down Level Design')
para('המערכת בנויה במבנה שכבות (MVC) עם הפרדת אחריות ברורה:')
layers = [
    ('שכבה','חבילות','תיאור'),
    ('1. Presentation (web/)','Controllers, DTOs, ExceptionHandler','ממשק HTTP REST — מקבל בקשות ומחזיר JSON'),
    ('2. Business Logic (service/)','RecommendationService, IndexService, IngestService','לוגיקה עסקית — אורקסטרציה של אלגוריתמים'),
    ('3. Data Access (db/)','Entities, Repositories, Converters','גישה ל-PostgreSQL דרך JPA/Hibernate'),
    ('4. Domain Model (model/)','Paper, User, Interaction','מודלי נתונים טהורים'),
    ('5. Algorithm (algorithm/)','Bm25Index, FusionReranker, MmrReranker','אלגוריתמים ב-Java'),
    ('6. Native (jni/ + native/)','HnswNative, BprNative, PageRankNative','גשר JNI לספריות C'),
    ('7. Integration (api/)','SemanticScholarClient, ArxivClient, OpenAlexClient','לקוחות API חיצוניים'),
]
add_table(layers[0], layers[1:])

page_break()

# ============================================================
# 13. USE CASES
# ============================================================
h(1, '11. תרשים מקרי-שימוש (UML Use Cases)')
para('שחקנים (Actors): משתמש (חוקר/סטודנט), מנהל (Admin), מערכת חיצונית (Semantic Scholar / ArXiv).', bold=True)
para('מקרי שימוש עיקריים:', bold=True)
cases = [
    ('UC-01: קבלת המלצות','משתמש','המשתמש מזין userId ו-query אופציונלי; המערכת מריצה את הצינור ההיברידי ומחזירה K מאמרים'),
    ('UC-02: רישום אינטראקציה','משתמש','לחיצה/צפייה/שמירה — מתועד ב-DB עם משקל'),
    ('UC-03: חיפוש טקסט','משתמש','המשתמש מקליד שאילתת חופשית; BM25 מדרג תוצאות'),
    ('UC-04: אכילת מאמרים (Ingest)','מנהל','חיפוש ב-S2/ArXiv ושמירה ל-DB; reindex אוטומטי'),
    ('UC-05: Deep Ingest','מנהל','אכילת מאמר + הפניותיו + ציטוטים'),
    ('UC-06: Reindex','מנהל','בנייה מחדש של HNSW+BM25+BPR+PageRank'),
    ('UC-07: צפייה בסטטיסטיקות','מנהל','צפייה בנתוני האינדקס, מאמרים, משתמשים'),
]
add_table(['תיאור','שחקן','מקרה שימוש'], cases)

page_break()

# ============================================================
# 14. DATA STRUCTURES
# ============================================================
h(1, '12. מבנה נתונים')
h(2, 'Paper')
add_table(['טיפוס','שדה','תיאור'],[
    ('UUID','id','מזהה ראשי'),
    ('String','s2Id, arxivId, doi','מזהים חיצוניים'),
    ('String','title, abstractText','מטא-נתונים'),
    ('Integer','year','שנת פרסום'),
    ('List<String>','topics, authorNames','נושאים ומחברים'),
    ('float[768]','embedding','וקטור SPECTER2 — מאוחסן כ-bytea (little-endian)'),
    ('Set<UUID>','citedPaperIds','גרף ציטוטים'),
])
doc.add_paragraph()
h(2, 'Interaction')
add_table(['טיפוס','שדה','תיאור'],[
    ('UUID','userId, paperId','FK למשתמש ולמאמר'),
    ('ActionType','actionType','VIEW/CLICK/SAVE/DOWNLOAD/CITE + משקל'),
    ('Instant','createdAt','חותמת זמן'),
])
doc.add_paragraph()
h(2, 'מבני נתונים נייטיבים (C)')
add_table(['מבנה','שדות','תיאור'],[
    ('HnswIndex','nodes[], dim, M, M0, ef_construction, mL, entry_point, max_level','גרף רב-שכבתי + נודים + שכנים'),
    ('HnswNode','vec[], level, layers[]','צומת בגרף + רשימות שכנים לכל שכבה'),
    ('HnswHeap','data[], size, cap, is_max','תור מין/מקס לחיפוש'),
    ('BprModel','user_factors[], item_factors[], num_factors','מטריצות פירוק למשתמשים ופריטים'),
])

page_break()

# ============================================================
# 15. DEVELOPMENT ENVIRONMENT
# ============================================================
h(1, '13. סביבת העבודה ושפת התכנות')
add_table(['פרט','ערך'],[
    ('שפה ראשית','Java 17'),
    ('שפה משנית','C (JNI נייטיב)'),
    ('סביבת פיתוח','IntelliJ IDEA'),
    ('מסגרת','Spring Boot 3.2.5'),
    ('כלי בנייה','Maven (Java), CMake (C)'),
    ('מסד נתונים','PostgreSQL 16 (Neon Cloud)'),
    ('מיגרציות','Flyway'),
    ('פורט','8080'),
    ('מערכת הפעלה','Windows'),
])

page_break()

# ============================================================
# 16. MAIN ALGORITHM — PSEUDOCODE
# ============================================================
h(1, '14. אלגוריתם ראשי (פסאודו קוד)')
h(2, 'Recommend(userId, query, k)')
code_para('FUNCTION Recommend(userId, query, k):')
code_para('    candidates = k * 5')
code_para('    seen = GetSeenPaperIds(userId)')
code_para('')
code_para('    // Signal 1: Content-Based (HNSW)')
code_para('    profile = UserProfileVector(userId)  // weighted avg with decay')
code_para('    IF profile != NULL:')
code_para('        hnswRanking = HNSW_Search(profile, candidates)')
code_para('')
code_para('    // Signal 2: Collaborative Filtering (BPR)')
code_para('    IF BPR_is_trained AND userId != NULL:')
code_para('        bprRanking = BPR_Recommend(userId, candidates, seen)')
code_para('')
code_para('    // Signal 3: Text Search (BM25)')
code_para('    IF query != NULL AND query != "":')
code_para('        tokens = Tokenize(query)')
code_para('        bm25Ranking = BM25_Search(tokens, candidates)')
code_para('')
code_para('    // Signal 4: Graph (PageRank)')
code_para('    prRanking = TopByPageRank(candidates)')
code_para('')
code_para('    // Fusion (RRF)')
code_para('    rankings = CollectNonEmpty(hnsw, bpr, bm25, pr)')
code_para('    fused = RRF(rankings, k=60)')
code_para('')
code_para('    // Diversity (MMR)')
code_para('    reranked = MMR(fused, embeddings, lambda=0.7, k*2)')
code_para('')
code_para('    // Exclude seen, convert to UUIDs, trim to k')
code_para('    result = []')
code_para('    FOR idx IN reranked:')
code_para('        paperId = IdxToUuid(idx)')
code_para('        IF paperId NOT IN seen: result.add(paperId)')
code_para('        IF result.size >= k: BREAK')
code_para('    RETURN result')

page_break()

# ============================================================
# 17. EXTERNAL INTERFACES / API DESCRIPTION
# ============================================================
h(1, '15. תיאור ממשקים חיצוניים — API')
apis = [
    ('Endpoint','Method','פרמטרים','תיאור'),
    ('/api/recommend','GET','userId?, q?, k=10','המלצות מותאמות אישית'),
    ('/api/ingest/s2','POST','query, limit=20','אכילת מאמרים מ-S2'),
    ('/api/ingest/s2/{id}','POST','maxRefs=50','Deep ingest + ציטוטים'),
    ('/api/ingest/arxiv','POST','query, maxResults=20','אכילת מאמרים מ-ArXiv'),
    ('/api/admin/reindex','POST','—','Rebuild כל האינדקסים'),
    ('/api/admin/stats','GET','—','סטטיסטיקות מערכת'),
    ('/api/admin/search-debug','GET','q','דיבאג BM25'),
    ('/api/papers','CRUD','—','ניהול מאמרים'),
    ('/api/users','CRUD','—','ניהול משתמשים'),
    ('/api/interactions','POST','—','רישום אינטראקציה'),
]
add_table(apis[0], apis[1:])

page_break()

# ============================================================
# 18. UML CLASS DIAGRAM (textual)
# ============================================================
h(1, '16. תרשים מחלקות (UML Class Diagram)')
para('[מומלץ לצרף תרשים UML באמצעות כלי כמו IntelliJ Diagrams או PlantUML]', sz=11, color=(128,128,128))
para('המחלקות העיקריות והקשרים ביניהן:', bold=True)
code_para('RecommendController --> RecommendationService')
code_para('RecommendationService --> IndexService')
code_para('RecommendationService --> InteractionService')
code_para('RecommendationService --> FusionReranker')
code_para('RecommendationService --> MmrReranker')
code_para('IndexService --> HnswNative (JNI)')
code_para('IndexService --> BprNative (JNI)')
code_para('IndexService --> PageRankNative (JNI)')
code_para('IndexService --> Bm25Index')
code_para('IndexService --> PaperRepository')
code_para('IngestService --> SemanticScholarClient')
code_para('IngestService --> ArxivClient')
code_para('IngestService --> PaperService')
code_para('PaperService --> PaperRepository --> PaperEntity')

page_break()

# ============================================================
# 19. MAIN CLASSES & FUNCTION TABLES
# ============================================================
h(1, '17. תיאור המחלקות הראשיות בפרויקט')

# ---- Bm25Index ----
h(2, 'Bm25Index')
add_table(['פונקציה','פרמטרים','מחזירה','תיאור','יעילות'],[
    ('addDocument','int docId, List<String> tokens','void','מוסיף מסמך לאינדקס הקדמי','O(|tokens|)'),
    ('build','(none)','void','בונה אינדקס הפוך + IDF','O(N·D)'),
    ('search','List<String> queryTerms, int k','List<Result>','מדרג כל מסמכים ומחזיר top-k','O(Q·df + N·log N)'),
    ('scoreDocument','int docId, List<String> q','double','ציון BM25 למסמך יחיד','O(Q·df)'),
])
doc.add_paragraph()

# ---- FusionReranker ----
h(2, 'FusionReranker')
add_table(['פונקציה','פרמטרים','מחזירה','תיאור','יעילות'],[
    ('reciprocalRankFusion','List<List<Long>> rankings, int k','List<Long>','ממזג רשימות ב-RRF','O(R·n + n·log n)'),
])
doc.add_paragraph()

# ---- MmrReranker ----
h(2, 'MmrReranker')
add_table(['פונקציה','פרמטרים','מחזירה','תיאור','יעילות'],[
    ('rerank','List<Long>, Map relevance, Map embeddings, double λ, int k','List<Long>','גיוון בחמדני MMR','O(k·|R|·dim)'),
    ('cosineSimilarity','float[] a, float[] b','double','דמיון קוסינוס','O(dim)'),
])
doc.add_paragraph()

# ---- HNSW (C) ----
h(2, 'HNSW (native C)')
add_table(['פונקציה','פרמטרים','מחזירה','תיאור','יעילות'],[
    ('hnsw_create','int dim, int M, int ef','HnswIndex*','יצירת אינדקס ריק','O(1)'),
    ('hnsw_add','HnswIndex*, float[] vec','int (nodeId)','הוספת וקטור: דגימת רמה, חיפוש שכנים, קשתות דו-כיווניות','O(M·log N·ef)'),
    ('hnsw_search','HnswIndex*, float[] q, int k, int ef','int[] (ids)','חיפוש k-NN: ירידה חמדנית + beam','O(ef·M·log N)'),
    ('hnsw_free','HnswIndex*','void','שחרור זיכרון','O(N)'),
])
doc.add_paragraph()

# ---- BPR (C) ----
h(2, 'BPR (native C)')
add_table(['פונקציה','פרמטרים','מחזירה','תיאור','יעילות'],[
    ('bpr_create','int users, int items, int factors, uint seed','BprModel*','אתחול מטריצות פירוק','O(U·F + I·F)'),
    ('bpr_fit','BprModel*, int[] users, int[] items, int n, int epochs, float lr, float reg','void','SGD pairwise ranking — Fisher-Yates shuffle, דגימה שלילית, עדכון גרדינט','O(epochs·n·F)'),
    ('bpr_recommend','BprModel*, int userId, int[] exclude, int k','int[] (top-k)','חישוב ציונים + partial selection sort','O(I·F + k·I)'),
    ('bpr_free','BprModel*','void','שחרור','O(1)'),
])
doc.add_paragraph()

# ---- PageRank (C) ----
h(2, 'PageRank (native C)')
add_table(['פונקציה','פרמטרים','מחזירה','תיאור','יעילות'],[
    ('pagerank_compute','int[] citing, int[] cited, int edges, int nodes, double d, int iter','double[] scores','איטרטיבי: out-degree, dangling mass, teleportation, התכנסות','O(iter·(E+N))'),
])
doc.add_paragraph()

# ---- RecommendationService ----
h(2, 'RecommendationService')
add_table(['פונקציה','פרמטרים','מחזירה','תיאור','יעילות'],[
    ('recommend','UUID userId, String q, int k','List<UUID>','צינור היברידי מלא (7 שלבים)','O(k·M·log N)'),
    ('fetchPapers','List<UUID> ids','List<PaperDto>','טעינת UUID ל-DTO בתוך טרנזקציה','O(k)'),
    ('recommendExplained','UUID, String, int','RecommendExplain','אותו דבר + פירוט אותות לדיבאג','O(k·M·log N)'),
])
doc.add_paragraph()

# ---- IndexService ----
h(2, 'IndexService')
add_table(['פונקציה','פרמטרים','מחזירה','תיאור','יעילות'],[
    ('reindex','(none)','void','בנייה מחדש של כל האינדקסים (AtomicBoolean guard)','O(N·M·log N)'),
    ('searchHnsw','float[] q, int k','List<Integer>','k-NN דרך JNI','O(ef·M·log N)'),
    ('searchBm25','List<String> tokens, int k','List<Integer>','חיפוש BM25','O(Q·df)'),
    ('recommendBpr','UUID userId, int k, Set<UUID> seen','List<Integer>','BPR top-k דרך JNI','O(I·F)'),
    ('userProfileVector','UUID, List<Interaction>','float[768]','ממוצע משוקלל עם דעיכה','O(|history|·768)'),
    ('topByPageRank','int k','List<Integer>','מיין ומחזיר top-k','O(N·log N)'),
])

page_break()

# ============================================================
# 20. MAIN PROGRAM PSEUDOCODE
# ============================================================
h(1, '18. התוכנית הראשית')
para('התוכנית הראשית היא PaperRecommenderApplication.java (מחלקת @SpringBootApplication) ו-IndexWarmUpRunner:')
code_para('PROGRAM PaperRecommenderApplication:')
code_para('    SpringApplication.run()      // Boot Spring context')
code_para('    DataLoader.run():')
code_para('        IF countPapers() < 10:')
code_para('            FOR topic IN ["NLP", "ML", "CV", "IR", "Graph"]:')
code_para('                ingest 8 papers from S2 per topic')
code_para('    IndexWarmUpRunner.run():')
code_para('        indexService.reindex()    // Build HNSW + BM25 + BPR + PageRank')
code_para('    // HTTP server on port 8080')
code_para('    SERVE static/index.html       // User UI')
code_para('    SERVE static/admin.html       // Admin console')
code_para('    LISTEN /api/**                // REST endpoints')

page_break()

# ============================================================
# 21. USER GUIDE
# ============================================================
h(1, '19. מדריך למשתמש')
h(2, 'התקנה והפעלה')
numbered('פתיחת הפרויקט ב-IntelliJ IDEA ולחיצה על Run')
numbered('המתנה עד שהאפליקציה עולה (Started PaperRecommenderApplication)')
numbered('פתיחת דפדפן בכתובת http://localhost:8080')
h(2, 'ממשק משתמש')
bullet('חיפוש: הקלדת שאילתת חיפוש ולחיצה על Search — המערכת מריצה את הצינור ההיברידי')
bullet('כרטיסי מאמרים: כל מאמר מוצג ככרטיס עם כותרת, תקציר, שנה ונושאים')
bullet('אינטראקציות: לחיצה (CLICK), צפייה של 2 שניות (VIEW), Save, Add to refs — נרשמות אוטומטית')
bullet('מאמרים שמורים והפניות: נשמרים ב-localStorage לפי משתמש')
h(2, 'קונסול ניהול (admin.html)')
bullet('Reindex — בנייה מחדש של כל האינדקסים')
bullet('Deep Ingest All — אכילת עומק של כל המאמרים + ציטוטים')
bullet('Stats — סטטיסטיקות מערכת')
bullet('Search Debug — דיבאג תוצאות BM25')

page_break()

# ============================================================
# 22. PERSONAL REFLECTION
# ============================================================
h(1, '20. סיכום אישי / רפלקציה')
para('[יש למלא חלק זה באופן אישי — לפחות חצי עמוד עד עמוד שלם. להלן נקודות מנחות:]', sz=11, color=(128,128,128))
doc.add_paragraph()
para('נקודות מנחות לרפלקציה:', bold=True)
bullet('כיצד הייתה עבורך העבודה על הפרויקט?')
bullet('מה קיבלת מהפרויקט?')
bullet('אילו כלים אתה לוקח איתך להמשך?')
bullet('מה הקשיים / אתגרים שעמדו בפניך?')
bullet('מה המסקנות שלך?')
bullet('מה היית עושה אחרת לו היית מתחיל היום?')
bullet('מה היית משנה בתהליך העבודה?')

page_break()

# ============================================================
# 23. BIBLIOGRAPHY (APA)
# ============================================================
h(1, '21. ביבליוגרפיה')
refs = [
    'Brin, S., & Page, L. (1998). The anatomy of a large-scale hypertextual web search engine. Computer Networks and ISDN Systems, 30(1-7), 107-117.',
    'Carbonell, J., & Goldstein, J. (1998). The use of MMR, diversity-based reranking for reordering documents and producing summaries. In Proceedings of the 21st Annual International ACM SIGIR Conference (pp. 335-336). ACM.',
    'Cormack, G. V., Clarke, C. L. A., & Buettcher, S. (2009). Reciprocal rank fusion outperforms Condorcet and individual rank learning methods. In Proceedings of the 32nd International ACM SIGIR Conference (pp. 758-759). ACM.',
    'Malkov, Y. A., & Yashunin, D. A. (2020). Efficient and robust approximate nearest neighbor search using Hierarchical Navigable Small World graphs. IEEE Transactions on Pattern Analysis and Machine Intelligence, 42(4), 824-836.',
    'Rendle, S., Freudenthaler, C., Gantner, Z., & Schmidt-Thieme, L. (2009). BPR: Bayesian personalized ranking from implicit feedback. In Proceedings of the 25th Conference on Uncertainty in Artificial Intelligence (pp. 452-461). AUAI Press.',
    'Robertson, S. E., & Zaragoza, H. (2009). The probabilistic relevance framework: BM25 and beyond. Foundations and Trends in Information Retrieval, 3(4), 333-389.',
    'Cohan, A., Feldman, S., Beltagy, I., Downey, D., & Weld, D. S. (2020). SPECTER: Document-level representation learning using citation-informed transformers. In Proceedings of the 58th Annual Meeting of the Association for Computational Linguistics (pp. 2270-2282). ACL.',
    'Spring Boot Reference Documentation. (2024). Spring Framework. https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/',
]
for ref in refs:
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Cm(1.27)
    p.paragraph_format.first_line_indent = Cm(-1.27)
    p.paragraph_format.space_after = Pt(6)
    r = p.add_run(ref); r.font.name='Arial'; r.font.size=Pt(11)

page_break()

# ============================================================
# 24. APPENDIX A — CODE
# ============================================================
h(1, '22. נספח א׳ — קוד הפרויקט')
para('[יש להדביק כאן את קוד הפרויקט כטקסט (לא כתמונה). הקבצים העיקריים:]', sz=11, color=(128,128,128))
bullet('algorithm/bm25/Bm25Index.java')
bullet('algorithm/rerank/FusionReranker.java')
bullet('algorithm/rerank/MmrReranker.java')
bullet('service/RecommendationService.java')
bullet('service/IndexService.java')
bullet('native/hnsw/hnsw.c')
bullet('native/bpr/bpr.c')
bullet('native/pagerank/pagerank.c')

# ============================================================
# SAVE
# ============================================================
out = r'C:\Users\ikomostars\IdeaProjects\PaperRecommender\PaperRecommender_Book_HE.docx'
doc.save(out)
print('OK:', out)

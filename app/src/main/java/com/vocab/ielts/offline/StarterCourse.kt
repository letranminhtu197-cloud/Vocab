package com.vocab.ielts.offline

/**
 * Khóa khởi đầu IELTS gọn nhẹ (60 từ, 6 chủ đề) do dự án tự soạn, dùng để người học
 * có thể bắt đầu ngay sau khi cài mà không cần đăng nhập, không cần mạng.
 *
 * Đây KHÔNG phải dữ liệu trích từ MochiVocab hay bất kỳ ứng dụng thương mại nào.
 * Người dùng có thể xóa từng từ hoặc bỏ qua khóa này và tự nhập dữ liệu của mình.
 */
object StarterCourse {
    const val NAME = "Khóa khởi đầu IELTS"
    const val VERSION = 1

    private fun w(
        term: String,
        pos: String,
        meaning: String,
        example: String,
        topic: String
    ) = WordEntity(
        term = term,
        partOfSpeech = pos,
        meaning = meaning,
        example = example,
        topic = topic
    )

    val words: List<WordEntity> = listOf(
        w("sustainable", "adj", "bền vững", "We need sustainable solutions to reduce waste.", "Environment"),
        w("emission", "n", "khí thải", "Cutting carbon emissions is a global priority.", "Environment"),
        w("renewable", "adj", "tái tạo", "Renewable energy is becoming cheaper every year.", "Environment"),
        w("biodiversity", "n", "đa dạng sinh học", "The forest supports rich biodiversity.", "Environment"),
        w("pollution", "n", "ô nhiễm", "Air pollution affects millions of city residents.", "Environment"),
        w("conservation", "n", "bảo tồn", "Wildlife conservation requires long-term funding.", "Environment"),
        w("deforestation", "n", "nạn phá rừng", "Deforestation destroys natural habitats.", "Environment"),
        w("ecosystem", "n", "hệ sinh thái", "Coral reefs are fragile ecosystems.", "Environment"),
        w("recycle", "v", "tái chế", "We recycle paper and glass at home.", "Environment"),
        w("climate", "n", "khí hậu", "Climate change is reshaping agriculture.", "Environment"),

        w("curriculum", "n", "chương trình giảng dạy", "The school revised its science curriculum.", "Education"),
        w("tuition", "n", "học phí", "University tuition has risen sharply.", "Education"),
        w("scholarship", "n", "học bổng", "She won a scholarship to study abroad.", "Education"),
        w("literacy", "n", "khả năng đọc viết", "Adult literacy programmes need more teachers.", "Education"),
        w("academic", "adj", "thuộc học thuật", "Academic writing requires a clear structure.", "Education"),
        w("assignment", "n", "bài tập được giao", "Submit your assignment before Friday.", "Education"),
        w("lecture", "n", "bài giảng", "The lecture covered climate policy.", "Education"),
        w("qualification", "n", "bằng cấp, chứng chỉ", "Employers value recognised qualifications.", "Education"),
        w("assessment", "n", "đánh giá", "Continuous assessment reduces exam pressure.", "Education"),
        w("vocational", "adj", "thuộc hướng nghiệp", "Vocational training prepares students for skilled jobs.", "Education"),

        w("innovation", "n", "sự đổi mới", "Innovation drives long-term economic growth.", "Technology"),
        w("automation", "n", "tự động hoá", "Automation is changing factory work.", "Technology"),
        w("algorithm", "n", "thuật toán", "The algorithm recommends relevant articles.", "Technology"),
        w("digital", "adj", "thuộc kỹ thuật số", "Digital skills are essential in most jobs.", "Technology"),
        w("privacy", "n", "sự riêng tư", "Users worry about data privacy.", "Technology"),
        w("cybersecurity", "n", "an ninh mạng", "Cybersecurity protects sensitive information.", "Technology"),
        w("device", "n", "thiết bị", "Mobile devices dominate internet access.", "Technology"),
        w("software", "n", "phần mềm", "The software updates automatically.", "Technology"),
        w("virtual", "adj", "ảo", "Virtual meetings are now routine.", "Technology"),
        w("infrastructure", "n", "cơ sở hạ tầng", "Rural areas lack digital infrastructure.", "Technology"),

        w("nutrition", "n", "dinh dưỡng", "Good nutrition supports brain development.", "Health"),
        w("sedentary", "adj", "ít vận động", "A sedentary lifestyle raises health risks.", "Health"),
        w("immunity", "n", "khả năng miễn dịch", "Vaccines strengthen immunity.", "Health"),
        w("chronic", "adj", "mãn tính", "Chronic diseases burden health systems.", "Health"),
        w("symptom", "n", "triệu chứng", "Fatigue is an early symptom.", "Health"),
        w("therapy", "n", "liệu pháp", "Physical therapy helped him recover.", "Health"),
        w("obesity", "n", "béo phì", "Childhood obesity is rising worldwide.", "Health"),
        w("diagnosis", "n", "chẩn đoán", "Early diagnosis improves survival rates.", "Health"),
        w("healthcare", "n", "chăm sóc sức khoẻ", "Access to healthcare varies by region.", "Health"),
        w("wellbeing", "n", "sức khoẻ tinh thần", "Balanced routines improve wellbeing.", "Health"),

        w("productivity", "n", "năng suất", "Flexible hours can boost productivity.", "Work & Economy"),
        w("recruitment", "n", "tuyển dụng", "Recruitment is expensive for small firms.", "Work & Economy"),
        w("wage", "n", "tiền công", "The minimum wage was raised last year.", "Work & Economy"),
        w("unemployment", "n", "thất nghiệp", "Unemployment fell during the boom.", "Work & Economy"),
        w("entrepreneur", "n", "doanh nhân", "The entrepreneur launched a green startup.", "Work & Economy"),
        w("revenue", "n", "doanh thu", "The company doubled its revenue.", "Work & Economy"),
        w("competitive", "adj", "có tính cạnh tranh", "The market is highly competitive.", "Work & Economy"),
        w("investment", "n", "sự đầu tư", "Investment in skills pays off.", "Work & Economy"),
        w("inflation", "n", "lạm phát", "Inflation reduces household purchasing power.", "Work & Economy"),
        w("outsourcing", "n", "thuê ngoài", "Outsourcing lowers production costs.", "Work & Economy"),

        w("inequality", "n", "bất bình đẳng", "Income inequality divides communities.", "Society"),
        w("urbanisation", "n", "đô thị hoá", "Rapid urbanisation strains public services.", "Society"),
        w("population", "n", "dân số", "The population is ageing quickly.", "Society"),
        w("migration", "n", "sự di cư", "Rural migration reshapes cities.", "Society"),
        w("welfare", "n", "phúc lợi", "The welfare system supports low-income families.", "Society"),
        w("discrimination", "n", "sự phân biệt đối xử", "Anti-discrimination laws protect workers.", "Society"),
        w("community", "n", "cộng đồng", "The community built a shared garden.", "Society"),
        w("volunteer", "v", "tình nguyện", "Students volunteer at local shelters.", "Society"),
        w("cultural", "adj", "thuộc văn hoá", "Cultural exchange broadens perspectives.", "Society"),
        w("policy", "n", "chính sách", "The policy aims to reduce traffic.", "Society")
    )
}

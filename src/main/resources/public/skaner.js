/**
 * Shtrix-kod / QR skanerni doimiy tinglaydi (fizik skaner klaviaturaga o'xshab
 * juda tez belgi "teradi" va Enter bilan tugatadi) va topilgan mahsulot sahifasiga
 * o'tkazadi. Mahsulot skanerlagan hodimga (uning magazin(lar)iga) tegishli bo'lmasa
 * ham sahifaga o'tkaziladi — ogohlantirish o'sha yerda (mahsulot-detail.html) ko'rsatiladi,
 * shu yerda emas, chunki foydalanuvchi baribir to'liq ma'lumotni ko'ra olishi kerak.
 *
 * Foydalanish: sahifaga shunchaki qo'shish yetarli:
 *   <script src="/skaner.js"></script>
 * (fragment.html'dagi `xabar()` global funksiyasi ishlatiladi — u sahifada bo'lishi shart)
 *
 * Modal (`.modal-overlay.open`) ochiq bo'lsa aralashmaydi — masalan mahsulotlar.html'dagi
 * "Maxsus kod" maydoniga skaner bilan yozish o'z alohida logikasiga ega, bu bilan
 * to'qnashmasligi kerak.
 *
 * Dashboard kabi sahifalar `window.skanerHolatYangilandi(matn, faol)` funksiyasini
 * belgilab, jonli holat ko'rsatkichini (masalan pulsatsiya qiluvchi nuqta) yangilashi mumkin.
 */
(function () {
    const MIN_KOD_UZUNLIK = 3;
    const SKANER_ORTACHA_MS = 50;   // odam qo'lda shuncha tez yoza olmaydi
    const BUFER_TUGASH_MS = 400;
    const TAYYOR_MATN = 'Skanerlashga tayyor';

    let buffer = '';
    let vaqtlar = [];
    let buferTaymer = null;

    function holatniYangila(matn, faol) {
        if (typeof window.skanerHolatYangilandi === 'function') {
            window.skanerHolatYangilandi(matn, !!faol);
        }
    }

    function tayyorgaQaytar() {
        holatniYangila(TAYYOR_MATN, false);
    }

    function bufferniTozala() {
        buffer = '';
        vaqtlar = [];
        clearTimeout(buferTaymer);
    }

    function modalOchiqmi() {
        return !!document.querySelector('.modal-overlay.open');
    }

    async function kodniQaytaIshla(kod) {
        holatniYangila('Qidirilmoqda: ' + kod, true);

        // QR kod to'liq havola bo'lib keladi (masalan /admin/mahsulot/{id}) — to'g'ridan-to'g'ri o'tamiz
        if (/^https?:\/\//i.test(kod)) {
            window.location.href = kod;
            return;
        }

        try {
            const res = await fetch('/admin/mahsulot-qidir-kod/' + encodeURIComponent(kod));
            if (!res.ok) {
                xabar('Bu kod bilan mahsulot topilmadi: ' + kod, 'warn');
                tayyorgaQaytar();
                return;
            }

            const data = await res.json();
            if (data.koplik) {
                window.location.href = '/admin/mahsulotlar?qidir=' + encodeURIComponent(kod);
                return;
            }
            // "Tegishli emas" ogohlantirishi mahsulot-detail.html'ning o'zida ko'rsatiladi —
            // shu yerda faqat o'sha sahifaga o'tkazamiz, ma'lumot baribir to'liq ko'rinadi
            window.location.href = '/admin/mahsulot/' + data.id;
        } catch (e) {
            xabar('Server bilan aloqa yo‘q', 'err');
            tayyorgaQaytar();
        }
    }

    function tekshirVaBajar() {
        const kod = buffer;
        const oraliqlar = vaqtlar;
        bufferniTozala();

        if (kod.length < MIN_KOD_UZUNLIK || oraliqlar.length < 2) return;

        let jami = 0;
        for (let i = 1; i < oraliqlar.length; i++) jami += oraliqlar[i] - oraliqlar[i - 1];
        const ortacha = jami / (oraliqlar.length - 1);
        if (ortacha > SKANER_ORTACHA_MS) return; // qo'lda yozilgan — e'tiborsiz qoldiramiz

        kodniQaytaIshla(kod);
    }

    document.addEventListener('keydown', function (e) {
        const tag = (document.activeElement && document.activeElement.tagName) || '';
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;
        if (modalOchiqmi()) return;

        if (e.key === 'Enter') {
            tekshirVaBajar();
            return;
        }
        if (e.key.length !== 1) return; // Shift, F1 va h.k. tugmalarni o'tkazib yuboramiz

        const hozir = Date.now();
        if (vaqtlar.length && hozir - vaqtlar[vaqtlar.length - 1] > 300) {
            bufferniTozala(); // uzoq tanaffus — yangi ketma-ketlik
        }
        buffer += e.key;
        vaqtlar.push(hozir);

        clearTimeout(buferTaymer);
        buferTaymer = setTimeout(bufferniTozala, BUFER_TUGASH_MS);
    });
})();

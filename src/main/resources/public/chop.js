/**
 * Universal "Chop etish" yordamchisi — QR kod / shtrix-kod kabi rasmlarni
 * bitta sahifada katakcha (grid) ko'rinishida chop etadi.
 *
 * Foydalanish: sahifaga qo'shish yetarli:
 *   <script src="/chop.js"></script>
 * so'ng istalgan joyda:
 *   chopEt(rasmUrl, sarlavhaMatni)                  — BITTA rasmni /admin/sozlamalar'da
 *                                                       belgilangan nusxa sonida (standart 4) takrorlab chop etadi
 *   chopEtRoyxat([{imgSrc, sarlavha}, ...])          — HAR XIL rasmlar ro'yxatini, har birini
 *                                                       BITTA martadan, bitta sahifaga chop etadi
 */
(function () {
    function esc(s) {
        return (s == null ? '' : String(s))
            .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;');
    }

    async function nusxaSoniniOl() {
        try {
            const res = await fetch('/admin/get-sozlamalar');
            if (res.ok) {
                const data = await res.json();
                if (data.chopEtishNusxaSoni > 0) return data.chopEtishNusxaSoni;
            }
        } catch (e) {
            // standart bilan davom etamiz
        }
        return 4;
    }

    /** @param items [{imgSrc, sarlavha}, ...] */
    function chopGrid(items, oynaSarlavhasi) {
        if (!items.length) return;

        const win = window.open('', '_blank', 'width=520,height=680');
        if (!win) {
            if (typeof xabar === 'function') {
                xabar('Chop etish oynasi ochilmadi — brauzer bloklagan bo‘lishi mumkin', 'err');
            }
            return;
        }

        const katakchalar = items.map(function (it) {
            return '<div class="katak"><img src="' + it.imgSrc + '" alt=""><div class="sarlavha">' +
                esc(it.sarlavha) + '</div></div>';
        }).join('');

        win.document.write(
            '<!DOCTYPE html><html><head><title>' + esc(oynaSarlavhasi || 'Chop etish') + '</title><style>' +
            'body{margin:0;padding:16px;font-family:Segoe UI,Tahoma,Geneva,Verdana,sans-serif;}' +
            '.setka{display:grid;grid-template-columns:repeat(2,1fr);gap:14px;}' +
            '.katak{text-align:center;border:1px dashed #999;border-radius:6px;padding:10px;break-inside:avoid;}' +
            '.katak img{max-width:100%;}' +
            '.sarlavha{margin-top:8px;font-size:12px;font-weight:600;color:#000;}' +
            '@media print{.katak{border:none;}}' +
            '</style></head><body><div class="setka">' + katakchalar + '</div></body></html>'
        );
        win.document.close();

        const rasmlar = win.document.querySelectorAll('img');
        let yuklandi = 0;
        rasmlar.forEach(function (img) {
            img.onload = img.onerror = function () {
                yuklandi++;
                if (yuklandi === rasmlar.length) {
                    win.focus();
                    win.print();
                }
            };
        });
        win.onafterprint = function () { win.close(); };
    }

    window.chopEt = async function (imgSrc, sarlavha) {
        const nusxaSoni = await nusxaSoniniOl();
        const items = [];
        for (let i = 0; i < nusxaSoni; i++) items.push({ imgSrc: imgSrc, sarlavha: sarlavha });
        chopGrid(items, sarlavha);
    };

    window.chopEtRoyxat = function (items, oynaSarlavhasi) {
        chopGrid(items, oynaSarlavhasi);
    };
})();

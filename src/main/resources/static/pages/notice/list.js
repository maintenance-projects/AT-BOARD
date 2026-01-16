(function () {
    const listEl = document.getElementById("noticeList");
    const emptyEl = document.getElementById("emptyState");

    function escapeHtml(v) {
        return String(v ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function setEmpty(isEmpty) {
        if (!emptyEl) return;
        emptyEl.hidden = !isEmpty;
    }

    function pad2(n) {
        return String(n).padStart(2, "0");
    }

    function formatDate(d) {
        const y = d.getFullYear();
        const m = pad2(d.getMonth() + 1);
        const day = pad2(d.getDate());
        const hh = pad2(d.getHours());
        const mm = pad2(d.getMinutes());
        return `${y}-${m}-${day} ${hh}:${mm}`;
    }

    function renderNoticeList(items) {
        if (!listEl) return;

        const arr = Array.isArray(items) ? items : [];
        if (arr.length === 0) {
            listEl.innerHTML = "";
            setEmpty(true);
            return;
        }

        setEmpty(false);

        listEl.innerHTML = arr
            .map((it, idx) => {
                const title = escapeHtml(it?.title);
                const writer = escapeHtml(it?.writer);
                const dateText = escapeHtml(it?.dateText);
                const num = escapeHtml(it?.num);

                const href = it?.href ? String(it.href) : "#";
                const id = it?.id ?? idx;

                return `
                        <li class="notice-item">
                            <a class="notice-link" href="${escapeHtml(href)}" data-id="${escapeHtml(id)}">
                            <div class="notice-left">
                                <div class="notice-title">${title}</div>
                                <div class="notice-sub">${writer}</div>
                            </div>
                            <div class="notice-right">
                                <div class="notice-sub">${num}</div>
                                <div class="notice-date">${dateText}</div>
                            </div>
                            </a>
                        </li>
                    `;
            })
            .join("");
    }

    function buildDemoData() {
        const writers = ["김민준", "김민수", "박지현", "이서연", "정우진"];
        const titles = [
            "국립병원정보팀 업무분장('26.1.15. 기준)",
            "국립병원정보팀 업무분장('26.1.12. 기준)",
            "데이터활용지원사업단 업무분장(26.1.12 기준)",
            "질병보건정보회사업단 업무분장(26.1.12 기준)",
            "PHR사업단 업무분장(26.1.12. 기준)",
        ];

        const base = new Date(2025, 11, 12, 12, 0, 0);
        return titles.map((t, i) => {
            const d = new Date(base.getTime());
            d.setMinutes(base.getMinutes() - i * 7);
            return {
                id: `N-${1000 + i}`,
                title: t,
                writer: writers[i % writers.length],
                num: 0,
                dateText: formatDate(d),
                href: `/pages/notice/detail?id=${encodeURIComponent(`N-${1000 + i}`)}`,
            };
        });
    }

    window.renderNoticeList = renderNoticeList;

    const initial = window.__NOTICE_DATA__;
    if (Array.isArray(initial) && initial.length) renderNoticeList(initial);
    else renderNoticeList(buildDemoData());

    document.addEventListener("click", function (e) {
        const btn = e.target.closest("[data-action='go-back']");
        if (!btn) return;
        if (history.length > 1) history.back();
        else location.href = "/";
    });
})();

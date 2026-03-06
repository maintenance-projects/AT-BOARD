// 게시판 관련 JavaScript
document.addEventListener('DOMContentLoaded', function() {
    console.log('게시판 페이지 로드됨');
});

// 아바타 배경색 팔레트 (글자 기반 해시로 배정)
var AVATAR_COLORS = [
    '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
    '#ec4899', '#06b6d4', '#f97316', '#14b8a6', '#6366f1',
    '#84cc16', '#e11d48'
];
function getAvatarColor(text) {
    var code = 0;
    for (var i = 0; i < text.length; i++) {
        code += text.charCodeAt(i);
    }
    return AVATAR_COLORS[code % AVATAR_COLORS.length];
}

// 프로필 사진 로드 (1초 타임아웃, 실패 시 글자 아바타 유지)
// container: 탐색 범위 (기본 document), 무한스크롤 추가 아이템에도 재사용 가능
function loadProfilePhotos(container) {
    var root = container || document;
    root.querySelectorAll('.profile-avatar[data-photo-url]').forEach(function(avatar) {
        if (avatar.dataset.photoLoaded) return; // 이미 시도한 경우 스킵
        var url = avatar.getAttribute('data-photo-url');
        if (!url) return;
        var img = avatar.querySelector('.profile-avatar__img');
        if (!img) return;
        avatar.dataset.photoLoaded = '1';
        // 글자 기반 배경색 배정
        var letterEl = avatar.querySelector('.profile-avatar__letter');
        var letterText = letterEl ? letterEl.textContent.trim() : '';
        if (letterText) avatar.style.background = getAvatarColor(letterText);
        var done = false;
        var timer = setTimeout(function() {
            if (done) return;
            done = true;
            img.onload = null;
            img.onerror = null;
            img.src = '';
        }, 1000);
        img.onload = function() {
            if (done) return;
            done = true;
            clearTimeout(timer);
            img.style.display = 'block';
            avatar.style.background = 'transparent';
            var letter = avatar.querySelector('.profile-avatar__letter');
            if (letter) letter.style.visibility = 'hidden';
        };
        img.onerror = function() {
            if (done) return;
            done = true;
            clearTimeout(timer);
        };
        img.src = url;
    });
}

// 이미지 로드 실패 시 공통 대체 이미지
var BOARD_IMG_FALLBACK = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='180' viewBox='0 0 300 180'%3E%3Crect width='300' height='180' fill='%23f5f5f5' rx='4'/%3E%3Crect x='100' y='50' width='100' height='70' fill='none' stroke='%23ccc' stroke-width='2' rx='3'/%3E%3Ccircle cx='122' cy='72' r='10' fill='%23ccc'/%3E%3Cpolyline points='100,115 128,82 152,100 175,68 200,115' fill='none' stroke='%23ccc' stroke-width='2'/%3E%3Ctext x='150' y='148' text-anchor='middle' fill='%23bbb' font-size='13' font-family='sans-serif'%3E%EC%9D%B4%EB%AF%B8%EC%A7%80%EB%A5%BC %EB%B6%88%EB%9F%AC%EC%98%AC %EC%88%98 %EC%97%86%EC%8A%B5%EB%8B%88%EB%8B%A4%3C/text%3E%3C/svg%3E";

// 게시글 본문 이미지 로드 실패 시 대체 이미지 표시
// container: 탐색 범위 (CSS selector 문자열 또는 Element)
function handleBrokenImages(container) {
    var root = typeof container === 'string' ? document.querySelector(container) : (container || document);
    if (!root) return;
    var FALLBACK = BOARD_IMG_FALLBACK;
    function applyFallback(img) {
        img.onerror = null;
        img.src = FALLBACK;
        img.style.maxWidth = '300px';
        img.style.width = '100%';
    }
    root.querySelectorAll('img').forEach(function(img) {
        if (img.dataset.brokenHandled) return;
        img.dataset.brokenHandled = '1';
        // 이미 로드 실패 상태인 경우 즉시 처리
        if (img.complete && img.naturalWidth === 0 && img.src && img.src !== FALLBACK) {
            applyFallback(img);
            return;
        }
        img.onerror = function() {
            applyFallback(this);
        };
    });
}

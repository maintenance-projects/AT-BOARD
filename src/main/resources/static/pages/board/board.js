// 게시판 관련 JavaScript
document.addEventListener('DOMContentLoaded', function() {
    console.log('게시판 페이지 로드됨');
});

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
        };
        img.onerror = function() {
            if (done) return;
            done = true;
            clearTimeout(timer);
        };
        img.src = url;
    });
}

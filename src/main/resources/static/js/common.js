/* ============================================================
   GetAPI - Common JavaScript
   순수 JS로 구현한 공통 인터랙션 (모바일 메뉴, 탭, 드롭다운 등)
   ============================================================ */

/**
 * 모바일 메뉴 토글
 */
function initMobileMenu() {
  var btn = document.getElementById('mobile-menu-btn');
  var menu = document.getElementById('mobile-menu');
  var iconOpen = document.getElementById('icon-menu-open');
  var iconClose = document.getElementById('icon-menu-close');

  if (!btn || !menu) return;

  btn.addEventListener('click', function () {
    var isOpen = menu.classList.contains('open');
    menu.classList.toggle('open');
    if (iconOpen && iconClose) {
      iconOpen.style.display = isOpen ? 'block' : 'none';
      iconClose.style.display = isOpen ? 'none' : 'block';
    }
  });

  /* 메뉴 내 링크 클릭 시 닫기 */
  var links = menu.querySelectorAll('a');
  links.forEach(function (link) {
    link.addEventListener('click', function () {
      menu.classList.remove('open');
      if (iconOpen && iconClose) {
        iconOpen.style.display = 'block';
        iconClose.style.display = 'none';
      }
    });
  });
}

/**
 * 탭 시스템
 * data-tab-group="그룹명" 안에서
 * data-tab="탭이름" (트리거), data-tab-content="탭이름" (콘텐츠)
 */
function initTabs() {
  var groups = document.querySelectorAll('[data-tab-group]');

  groups.forEach(function (group) {
    var triggers = group.querySelectorAll('[data-tab]');
    var contents = group.querySelectorAll('[data-tab-content]');

    triggers.forEach(function (trigger) {
      trigger.addEventListener('click', function () {
        var tabName = this.getAttribute('data-tab');

        /* 트리거 활성화 */
        triggers.forEach(function (t) { t.classList.remove('active'); });
        this.classList.add('active');

        /* 콘텐츠 활성화 */
        contents.forEach(function (c) { c.classList.remove('active'); });
        var target = group.querySelector('[data-tab-content="' + tabName + '"]');
        if (target) target.classList.add('active');
      });
    });
  });
}

/**
 * 드롭다운 메뉴
 * data-dropdown="이름" (트리거), data-dropdown-menu="이름" (메뉴)
 */
function initDropdowns() {
  var triggers = document.querySelectorAll('[data-dropdown]');

  triggers.forEach(function (trigger) {
    var menuName = trigger.getAttribute('data-dropdown');
    var menu = document.querySelector('[data-dropdown-menu="' + menuName + '"]');
    if (!menu) return;

    trigger.addEventListener('click', function (e) {
      e.stopPropagation();
      /* 다른 드롭다운 닫기 */
      document.querySelectorAll('.dropdown-menu.open').forEach(function (m) {
        if (m !== menu) m.classList.remove('open');
      });
      menu.classList.toggle('open');
    });
  });

  /* 바깥 클릭 시 모든 드롭다운 닫기 */
  document.addEventListener('click', function () {
    document.querySelectorAll('.dropdown-menu.open').forEach(function (m) {
      m.classList.remove('open');
    });
  });
}

/* ── Profile Dropdown Toggle ── */
function initProfileDropdown() {
  var profileBtn = document.getElementById('profile-btn');
  var profileDropdown = document.getElementById('profile-dropdown');

  if (profileBtn && profileDropdown) {
    profileBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      profileDropdown.classList.toggle('open');
    });
    document.addEventListener('click', function (e) {
      if (!profileDropdown.contains(e.target) && !profileBtn.contains(e.target)) {
        profileDropdown.classList.remove('open');
      }
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') profileDropdown.classList.remove('open');
    });
  }
}

/**
 * 다이얼로그(모달)
 * data-dialog-open="이름" (열기 버튼), data-dialog-close="이름" (닫기 버튼)
 * id="dialog-이름" (오버레이)
 */
function initDialogs() {
  document.querySelectorAll('[data-dialog-open]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var name = this.getAttribute('data-dialog-open');
      var overlay = document.getElementById('dialog-' + name);
      if (overlay) overlay.classList.add('open');
    });
  });

  document.querySelectorAll('[data-dialog-close]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var name = this.getAttribute('data-dialog-close');
      var overlay = document.getElementById('dialog-' + name);
      if (overlay) overlay.classList.remove('open');
    });
  });

  /* 오버레이 클릭으로 닫기 */
  document.querySelectorAll('.dialog-overlay').forEach(function (overlay) {
    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) overlay.classList.remove('open');
    });
  });
}

/**
 * 스위치 토글
 * .switch 클래스 요소 클릭 시 .active 토글
 */
function initSwitches() {
  document.querySelectorAll('.switch:not([data-managed])').forEach(function (sw) {
    sw.addEventListener('click', function () {
      this.classList.toggle('active');
    });
  });
}

/**
 * 클립보드 복사
 * data-copy="복사할텍스트" 또는 data-copy-from="선택자"
 */
function initCopyButtons() {
  document.querySelectorAll('[data-copy]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var text = this.getAttribute('data-copy');
      var sourceSelector = this.getAttribute('data-copy-from');

      if (sourceSelector) {
        var source = document.querySelector(sourceSelector);
        if (source) text = source.textContent;
      }

      if (text && navigator.clipboard) {
        navigator.clipboard.writeText(text);
        var originalHtml = this.innerHTML;
        this.innerHTML = '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg> 복사됨';
        var self = this;
        setTimeout(function () {
          self.innerHTML = originalHtml;
        }, 2000);
      }
    });
  });
}

/**
 * 좋아요 버튼 토글
 */
function initLikeButtons() {
  document.querySelectorAll('[data-like]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var isLiked = this.classList.contains('liked');
      this.classList.toggle('liked');

      /* 카운트 업데이트 */
      var text = this.textContent.trim();
      var match = text.match(/(\d[\d,]*)/);
      if (match) {
        var count = parseInt(match[1].replace(/,/g, ''), 10);
        count = isLiked ? count - 1 : count + 1;
        var formatted = count.toLocaleString();
        this.innerHTML = this.innerHTML.replace(/(\d[\d,]*)/, formatted);
      }

      /* 스타일 토글 */
      if (isLiked) {
        this.classList.remove('btn-primary');
        this.classList.add('btn-outline');
      } else {
        this.classList.remove('btn-outline');
        this.classList.add('btn-primary');
      }
    });
  });
}

/**
 * 라디오 옵션 선택
 */
function initRadioOptions() {
  var groups = document.querySelectorAll('[data-radio-group]');
  groups.forEach(function (group) {
    var options = group.querySelectorAll('.radio-option');
    options.forEach(function (option) {
      option.addEventListener('click', function () {
        options.forEach(function (o) {
          o.classList.remove('selected');
          var dot = o.querySelector('.radio-dot');
          if (dot) dot.classList.remove('selected');
        });
        this.classList.add('selected');
        var dot = this.querySelector('.radio-dot');
        if (dot) dot.classList.add('selected');
      });
    });
  });
}

/**
 * 커스텀 셀렉트 (팝오버 드롭다운)
 * .custom-select 래퍼 안에
 *   .custom-select-trigger (버튼)
 *   .custom-select-popover > .custom-select-item[data-value] (항목)
 *
 * 옵션: data-cs-callback="함수명" - 선택 시 전역 콜백 호출
 */
function initCustomSelects() {
  var selects = document.querySelectorAll('.custom-select');
  selects.forEach(function (wrapper) {
    var trigger = wrapper.querySelector('.custom-select-trigger');
    var popover = wrapper.querySelector('.custom-select-popover');
    var items = wrapper.querySelectorAll('.custom-select-item');
    var labelEl = trigger.querySelector('.cs-value');
    if (!trigger || !popover) return;

    trigger.addEventListener('click', function (e) {
      e.stopPropagation();
      // 다른 팝오버 닫기
      document.querySelectorAll('.custom-select-popover.open').forEach(function (p) {
        if (p !== popover) {
          p.classList.remove('open');
          p.closest('.custom-select').querySelector('.custom-select-trigger').classList.remove('open');
        }
      });
      var isOpen = popover.classList.contains('open');
      popover.classList.toggle('open');
      trigger.classList.toggle('open');
    });

    items.forEach(function (item) {
      item.addEventListener('click', function (e) {
        e.stopPropagation();
        var value = this.getAttribute('data-value');
        var label = this.querySelector('.cs-label').textContent;

        // 선택 상태 갱신
        items.forEach(function (i) { i.classList.remove('selected'); });
        this.classList.add('selected');

        // 트리거 텍스트 갱신
        if (labelEl) labelEl.textContent = label;

        // 닫기
        popover.classList.remove('open');
        trigger.classList.remove('open');

        // 콜백 호출
        var callbackName = wrapper.getAttribute('data-cs-callback');
        if (callbackName && typeof window[callbackName] === 'function') {
          window[callbackName](value, label, wrapper);
        }
      });
    });
  });

  // 바깥 클릭 시 모두 닫기
  document.addEventListener('click', function () {
    document.querySelectorAll('.custom-select-popover.open').forEach(function (p) {
      p.classList.remove('open');
      var t = p.closest('.custom-select');
      if (t) t.querySelector('.custom-select-trigger').classList.remove('open');
    });
  });
}

/**
 * 태그 입력 시스템
 * .tag-input-wrapper 안에
 *   input.tag-input, .tag-list (태그 표시 영역)
 *   data-max-tags="5" (최대 태그 수)
 */
function initTagInputs() {
  document.querySelectorAll('.tag-input-wrapper').forEach(function (wrapper) {
    var input = wrapper.querySelector('.tag-input');
    var list = wrapper.querySelector('.tag-list');
    var maxTags = parseInt(wrapper.getAttribute('data-max-tags')) || 5;
    var tags = [];

    if (!input || !list) return;

    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ',') {
        e.preventDefault();
        var val = this.value.trim().replace(/^#/, '');
        if (val && tags.length < maxTags && tags.indexOf(val) === -1) {
          tags.push(val);
          renderTags();
        }
        this.value = '';
      }
      if (e.key === 'Backspace' && !this.value && tags.length > 0) {
        tags.pop();
        renderTags();
      }
    });

    function renderTags() {
      list.innerHTML = '';
      tags.forEach(function (tag, i) {
        var el = document.createElement('span');
        el.className = 'badge badge-secondary';
        el.style.cssText = 'display:inline-flex;align-items:center;gap:0.25rem;cursor:pointer';
        el.innerHTML = '#' + tag + ' <svg style="width:0.625rem;height:0.625rem" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>';
        el.addEventListener('click', function () {
          tags.splice(i, 1);
          renderTags();
        });
        list.appendChild(el);
      });
    }

    var form = wrapper.closest('form');
    if (form) {
      form.addEventListener('submit', function () {
        input.value = tags.join(',');
      });
    }
  });
}
/**
 * 셀렉트 값이 바뀔 때마다 실행되는 함수
 */
/*window.onFilterChange = function(value, label, wrapper) {
    console.log("선택된 값:", value); // all, pending, done 등
    
    // 예: 탭 메뉴처럼 특정 컨텐츠만 보여주기
    // 모든 컨텐츠 섹션을 숨기고 선택된 ID만 보여주는 로직
    if (value === 'all') {
        document.querySelectorAll('.post-item').forEach(el => el.style.display = 'block');
    } else {
        document.querySelectorAll('.post-item').forEach(el => {
            // 요소의 data-status 값과 비교하여 필터링
            el.style.display = (el.getAttribute('data-status') === value) ? 'block' : 'none';
        });
    }

    // 또는 서버에서 데이터를 새로 받아오고 싶다면?
    // location.href = "/admin/posts?status=" + value;
};*/

/**
 * 공유 버튼 - 클립보드에 현재 URL 복사
 */
function initShareButtons() {
  document.querySelectorAll('[data-share]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var url = window.location.href;
      if (navigator.clipboard) {
        navigator.clipboard.writeText(url);
      }
      var orig = this.innerHTML;
      this.innerHTML = '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg> URL 복사됨';
      var self = this;
      setTimeout(function () { self.innerHTML = orig; }, 2000);
    });
  });
}

/**
 * 댓글 좋아요 토글
 */
function initCommentLikes() {
  document.querySelectorAll('.comment-like-btn, .card .btn-ghost').forEach(function (btn) {
    if (!btn.querySelector('svg path[d*="M7 10v12"]')) return;
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      var isLiked = this.classList.contains('liked');
      this.classList.toggle('liked');
      var text = this.textContent.trim();
      var count = parseInt(text, 10);
      if (!isNaN(count)) {
        count = isLiked ? count - 1 : count + 1;
        var svg = this.querySelector('svg').outerHTML;
        this.innerHTML = svg + ' ' + count;
      }
      if (isLiked) {
        this.style.color = '';
      } else {
        this.style.color = 'var(--primary)';
      }
    });
  });
}

/**
 * Toast 알림 시스템
 */
function showToast(message, type) {
  type = type || 'default';
  var container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.style.cssText = 'position:fixed;bottom:1.5rem;right:1.5rem;z-index:9999;display:flex;flex-direction:column;gap:0.5rem;pointer-events:none';
    document.body.appendChild(container);
  }
  var toast = document.createElement('div');
  toast.className = 'toast-item';
  var bgColor = type === 'success' ? 'var(--primary)' : type === 'error' ? 'hsl(0, 84.2%, 60.2%)' : 'var(--card)';
  var textColor = type === 'default' ? 'var(--foreground)' : 'white';
  toast.style.cssText = 'background:' + bgColor + ';color:' + textColor + ';padding:0.75rem 1.25rem;border-radius:var(--radius-lg);font-size:0.875rem;box-shadow:0 8px 30px rgba(0,0,0,0.4);border:1px solid var(--border);pointer-events:auto;opacity:0;transform:translateY(0.5rem);transition:all 0.3s ease';
  toast.textContent = message;
  container.appendChild(toast);
  requestAnimationFrame(function () {
    toast.style.opacity = '1';
    toast.style.transform = 'translateY(0)';
  });
  setTimeout(function () {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(0.5rem)';
    setTimeout(function () { toast.remove(); }, 300);
  }, 3000);
}

/**
 * 폼 submit 버튼 피드백 (loading 상태)
 */
function initFormSubmits() {
  document.querySelectorAll('form[onsubmit="return false"]').forEach(function (form) {
    var submitBtn = form.querySelector('[type="submit"]');
    if (!submitBtn || submitBtn.tagName === 'A') return;

    submitBtn.addEventListener('click', function (e) {
      e.preventDefault();
      var originalText = this.textContent;
      this.disabled = true;
      this.innerHTML = '<div style="width:1rem;height:1rem;border:2px solid currentColor;border-top-color:transparent;border-radius:50%;animation:spin 1s linear infinite"></div> 처리 중...';
      var self = this;
      setTimeout(function () {
        self.disabled = false;
        self.textContent = originalText;
        showToast('성공적으로 처리되었습니다.', 'success');
      }, 1500);
    });
  });
}

/**
 * Accordion / Collapsible
 * data-accordion 클릭 시 data-accordion-content 토글
 */
function initAccordions() {
  document.querySelectorAll('[data-accordion]').forEach(function (trigger) {
    trigger.addEventListener('click', function () {
      var name = this.getAttribute('data-accordion');
      var content = document.querySelector('[data-accordion-content="' + name + '"]');
      if (!content) return;
      var isOpen = content.classList.contains('open');
      content.classList.toggle('open');
      this.classList.toggle('open');
      if (isOpen) {
        content.style.maxHeight = '0';
      } else {
        content.style.maxHeight = content.scrollHeight + 'px';
      }
    });
  });
}

/**
 * Tooltip
 * data-tooltip="텍스트" 마우스 오버 시 표시
 */
function initTooltips() {
  document.querySelectorAll('[data-tooltip]').forEach(function (el) {
    var tipEl = null;
    el.addEventListener('mouseenter', function () {
      var text = this.getAttribute('data-tooltip');
      tipEl = document.createElement('div');
      tipEl.className = 'tooltip-popup';
      tipEl.textContent = text;
      tipEl.style.cssText = 'position:fixed;z-index:9999;padding:0.375rem 0.625rem;border-radius:var(--radius);background:var(--foreground);color:var(--background);font-size:0.75rem;white-space:nowrap;pointer-events:none;opacity:0;transition:opacity 0.15s';
      document.body.appendChild(tipEl);
      var rect = this.getBoundingClientRect();
      tipEl.style.left = rect.left + rect.width / 2 - tipEl.offsetWidth / 2 + 'px';
      tipEl.style.top = rect.top - tipEl.offsetHeight - 6 + 'px';
      requestAnimationFrame(function () { if (tipEl) tipEl.style.opacity = '1'; });
    });
    el.addEventListener('mouseleave', function () {
      if (tipEl) { tipEl.remove(); tipEl = null; }
    });
  });
}

/**
 * 페이지 로드 시 모든 컴포넌트 초기화
 */
document.addEventListener('DOMContentLoaded', function () {
  initMobileMenu();
  initTabs();
  initDropdowns();
  initProfileDropdown();
  initDialogs();
  initSwitches();
  initCopyButtons();
  initLikeButtons();
  initRadioOptions();
  initCustomSelects();
  initTagInputs();
  initShareButtons();
  initCommentLikes();
  initFormSubmits();
  initAccordions();
  initTooltips();
});

/**
 * Reset bugs related context.
 */
r.bugs.reset = function() {
    // Hiding bugs container
    $('#bugs-container').hide();
};
  
/**
 * Initializing bugs module.
 */
r.bugs.init = function() {
    // Listening hash changes on #/bugs/*
    // /bugs/
    $.History.bind('/bugs/', function(state, target) {
        // Resetting page context
        r.main.reset();
        
        // Showing bugs container
        $('#bugs-container').show();
        
        // Configuring contextual toolbar
        $('#toolbar > .bugs').removeClass('hidden');
        
        if (r.user.hasBaseFunction('ADMIN')) {
        // Loading logs
        r.bugs.loadLogs(false);
        } else {
        // User is not admin, hide related features
        $('#bugs-container .admin').hide();
        }

        r.bugs.loadList();
    });

    // Rebuild index button click
    $('#bugs-container .rebuild-index-button').click(function() {
        r.util.ajax({
        url: r.util.url.app_batch_reindex,
        type: 'POST',
        done: function(data) {
            $().toastmessage('showSuccessToast', $.t('bugs.rebuildindex.success'));
        }
        });
    });

    // Logs infinite scrolling
    $('#logs-container').scroll(function() {
        if ($('#logs-table tr.log-item:last').visible(true)) {
        r.bugs.loadLogs(true);
        }
    });

    // Reload logs on level change
    $('#logs-level-select').change(function() {
        r.bugs.loadLogs(false);
    });

    // Reload logs on refresh button click
    $('#logs-refresh-button').click(function() {
        r.bugs.loadLogs(false);
    });

    r.bugs.loadList = function() {
        $.ajax({
          url: r.util.url.bugs,
          method: 'GET',
          dataType: 'json',  // Ensure the response is parsed as JSON
          success: function(data) {
            // Check if data is an array; if not, try to parse or use the appropriate property
            if (!Array.isArray(data)) {
              console.error('Expected an array, got:', data);
              return;
            }
            let $bugTable = $('#bugs-list-table');
            $bugTable.empty();
        
            data.forEach(function(bug) {
              let statusText = bug.status ? bug.status : 'OPEN';
              let row = `
                <tr>
                  <td>${bug.description}</td>
                  <td>${statusText}</td>
                  <td><button class="delete-bug" data-id="${bug.id}">Delete</button></td>
                  <td><button class="resolve-bug" data-id="${bug.id}">Resolve</button></td>
                </tr>
              `;
              $bugTable.append(row);
            });
        
            // Attach button event handlers after table is populated
            $('.delete-bug').on('click', function() {
              let bugId = $(this).data('id');
              $.ajax({
                url: r.util.url.bugs_delete.replace('{id}', bugId),
                method: 'DELETE',
                success: function() {
                  r.bugs.loadList();
                }
              });
            });
        
            $('.resolve-bug').on('click', function() {
              let bugId = $(this).data('id');
              $.ajax({
                url: r.util.url.bugs_resolve.replace('{id}', bugId),
                method: 'POST',
                success: function() {
                  r.bugs.loadList();
                }
              });
            });
          },
          error: function(xhr, status, error) {
            console.error(status, xhr, 'Error fetching bugs:', error);
          }
        });
      };

    $('#bug-submit-button').click(function() {
        let bugDescription = $('#bug-description').val();
        console.log('Bug description:', bugDescription);
        if (bugDescription) {
            $.ajax({
                // TODO: Update URL
                url: r.util.url.bugs,
                method: 'POST',
                data: { description: bugDescription },
                success: function() {
                    console.log('Bug added successfully');
                    // Optionally reload the bug list
                    r.bugs.loadList();
                }
            });
        }
    });
};

/**
 * Fetch and display bugs with "Delete" and "Resolve" buttons.
 */

/**
 * Show update label if needed.
 */
r.bugs.showUpdate = function(currentVersion, tag, tagDate) {
    var date = moment(tagDate);
    var diff = moment().diff(date);


    if (diff > 3600000 * 24 && r.bugs.normalizeTag(currentVersion) < r.bugs.normalizeTag(tag)) {
        $('#subscriptions .update, #bugs-version-new')
        .show()
        .html('<a href="http://www.sismics.com/reader/" target="_blank">' + $.t('bugs.newupdate') + ': ' + tag + '</a>');
    }
};

/**
 * Transform a tag in int value.
 */
r.bugs.normalizeTag = function(tag) {
    var out = parseInt(tag.replace(/\./g, ''));
    if (out < 10) out *= 100;
    if (out < 100) out *= 10;
    return out;
};

/**
 * Load logs.
 */
r.bugs.logsLoading = false;
r.bugs.loadLogs = function(next) {
    // Stop if already loading something
    if (r.bugs.logsLoading) {
        return;
    }

    // Check if there is more to load
    var count = 0;
    if (next) {
        var total = $('#logs-table').data('total');
        count = $('#logs-table tr.log-item').length;
        if (count >= total) {
        return;
        }
    }

    // Calling API
    r.bugs.logsLoading = true;
    r.util.ajax({
        url: r.util.url.app_log,
        type: 'GET',
        data: { limit: 100, offset: next ? count : 0, level: $('#logs-level-select').val() },
        done: function(data) {
        // Building table rows
        var html = '';
        $(data.logs).each(function(i, log) {
            html += '<tr class="log-item ' + log.level.toLowerCase() + '">'
                + '<td class="date">' + moment(log.date).format('YYYY-MM-DD HH:mm:ss') + '</td>'
                + '<td class="level">' + log.level + '</td>'
                + '<td class="tag">' + log.tag + '</td>'
                + '<td class="message">' + log.message + '</td>'
            '</tr>';
        });
        
        // Add or replace new rows
        if (next) {
            $('#logs-table').append(html);
        } else {
            $('#logs-table').html(html);
            $('#logs-table').data('total', data.total)
        }
        },
        always: function() {
        r.bugs.logsLoading = false;
        }
    });
};